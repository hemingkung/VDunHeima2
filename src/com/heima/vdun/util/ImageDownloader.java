package com.heima.vdun.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import com.heima.vdun.util.Logger;
import android.widget.ImageView;

import com.heima.vdun.global.GlobalConstants;
import com.heima.vdun.service.HttpManager;


/**
 * This helper class download images from the Internet and binds those with the
 * provided ImageView.
 * 
 * <p>
 * It requires the INTERNET permission, which should be added to your
 * application's manifest file.
 * </p>
 * 
 * A local cache of downloaded images is maintained internally to improve
 * performance.
 */
public class ImageDownloader {
	private static final String LOG_TAG = "ImageDownloader";

	private static final int HARD_CACHE_CAPACITY = 40;
	private static final int DELAY_BEFORE_PURGE = 30 * 1000; // in milliseconds
	private static final int IO_BUFFER_SIZE = 4 * 1024;

	// Hard cache, with a fixed maximum capacity and a life duration
	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
			HARD_CACHE_CAPACITY / 2, 0.75f, true) {
		/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(
				LinkedHashMap.Entry<String, Bitmap> eldest) {
			if (size() > HARD_CACHE_CAPACITY) {
				// Entries push-out of hard reference cache are transferred to
				// soft reference cache
				sSoftBitmapCache.put(eldest.getKey(),
						new SoftReference<Bitmap>(eldest.getValue()));
				return true;
			} else
				return false;
		}
	};

	// Soft cache for bitmap kicked out of hard cache
	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			HARD_CACHE_CAPACITY / 2);

	private final Handler purgeHandler = new Handler();

	private final Runnable purger = new Runnable() {
		public void run() {
			clearCache();
		}
	};

	/**
	 * Download the specified image from the Internet and binds it to the
	 * provided ImageView. The binding is immediate if the image is found in the
	 * cache and will be done asynchronously otherwise. A null bitmap will be
	 * associated to the ImageView if an error occurs.
	 * 
	 * @param url
	 *            The URL of the image to download.
	 * @param imageView
	 *            The ImageView to bind the downloaded image to.
	 */
	public void download(String url, ImageView imageView) {
		download(url, imageView, null);
	}

	/**
	 * Same as {@link #download(String, ImageView)}, with the possibility to
	 * provide an additional cookie that will be used when the image will be
	 * retrieved.
	 * 
	 * @param url
	 *            The URL of the image to download.
	 * @param imageView
	 *            The ImageView to bind the downloaded image to.
	 * @param cookie
	 *            A cookie String that will be used by the http connection.
	 */
	public void download(String url, ImageView imageView, String cookie) {
		resetPurgeTimer();
		Bitmap bitmap = getBitmapFromCache(url);
		//Logger.d(LOG_TAG, "bitmap: " + bitmap);
		if (bitmap == null) {
			forceDownload(url, imageView, cookie);
		} else {
			cancelPotentialDownload(url, imageView);
			imageView.setImageBitmap(bitmap);
		}
	}

	/*
	 * Same as download but the image is always downloaded and the cache is not
	 * used. Kept private at the moment as its interest is not clear. private
	 * void forceDownload(String url, ImageView view) { forceDownload(url, view,
	 * null); }
	 */

	/**
	 * Same as download but the image is always downloaded and the cache is not
	 * used. Kept private at the moment as its interest is not clear.
	 */
	private void forceDownload(String url, ImageView imageView, String cookie) {
		// State sanity: url is guaranteed to never be null in
		// DownloadedDrawable and cache keys.
		if (url == null) {
			return;
		}

		if (cancelPotentialDownload(url, imageView)) {
			BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
			DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
			imageView.setImageDrawable(downloadedDrawable);
			task.execute(url, cookie);
		}else {
			//imageView.setImageResource(R.drawable.default_icon);
		}
	}

	/**
	 * Clears the image cache used internally to improve performance. Note that
	 * for memory efficiency reasons, the cache will automatically be cleared
	 * after a certain inactivity delay.
	 */
	public void clearCache() {
		sHardBitmapCache.clear();
		sSoftBitmapCache.clear();
	}

	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks(purger);
		purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
	}

	/**
	 * Returns true if the current download has been canceled or if there was no
	 * download in progress on this image view. Returns false if the download in
	 * progress deals with the same url. The download is not stopped in that
	 * case.
	 */
	private static boolean cancelPotentialDownload(String url,
			ImageView imageView) {
		BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

		if (bitmapDownloaderTask != null) {
			String bitmapUrl = bitmapDownloaderTask.url;
			if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
				bitmapDownloaderTask.cancel(true);
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	/**
	 * @param imageView
	 *            Any imageView
	 * @return Retrieve the currently active download task (if any) associated
	 *         with this imageView. null if there is no such task.
	 */
	private static BitmapDownloaderTask getBitmapDownloaderTask(
			ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	/**
	 * @param url
	 *            The URL of the image that will be retrieved from the cache.
	 * @return The cached bitmap or null if it was not found.
	 */
	private Bitmap getBitmapFromCache(String url) {
		// First try the hard reference cache
		synchronized (sHardBitmapCache) {
			final Bitmap bitmap = sHardBitmapCache.get(url);
			if (bitmap != null) {
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sHardBitmapCache.remove(url);
				sHardBitmapCache.put(url, bitmap);
				return bitmap;
			}
		}

		// Then try the soft reference cache
		SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
		if (bitmapReference != null) {
			final Bitmap bitmap = bitmapReference.get();
			if (bitmap != null) {
				// Bitmap found in soft cache
				return bitmap;
			} else {
				// Soft reference has been Garbage Collected
				sSoftBitmapCache.remove(url);
			}
		}

		return null;
	}

	public static String getCacheFilePath(String url) {
		String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
		String dir = GlobalConstants.SAVE_DIR;

		return dir + fileName;
	}

	public static String getPicFilePath(String name) {
		String dir = GlobalConstants.SAVE_DIR;

		return dir + name;
	}

	public static void saveBitmapToLocalCache(String path, Bitmap bmp) {
		String state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			return;
		}

		Logger.d(LOG_TAG, "path: " + path);
		File file = new File(path);
		//Logger.i(LOG_TAG, " dir: " + file.mkdirs());
		File dir = new File(file.getParent());
		Logger.i(LOG_TAG, " dir: " + dir.getAbsolutePath());
		if (!dir.isDirectory()) {
			if (dir.mkdirs()) {
				Logger.i(LOG_TAG, "mkdir ok. dir: " + file.getParent());
				return;
			}else {
				Logger.i(LOG_TAG, "mkdir failed. dir: " + file.getParent());
			}
		}
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Logger.i(LOG_TAG, "create file failed.");
				return;
			}

			try {
				FileOutputStream os = new FileOutputStream(file);
				if (!bmp.compress(CompressFormat.PNG, 100, os)) {
					Logger.i(LOG_TAG, "failed. save bitmap to local cache.");
				}
				os.flush();
				os.close();
			} catch (FileNotFoundException e) {
				Logger.e(LOG_TAG, "file not found. file: " + path);
			} catch (IOException e) {
				Logger.e(LOG_TAG, e.getMessage());
			} catch (Exception e) {
				Logger.e(LOG_TAG,
						"failed exception. save bitmap to local cahce.");
			}
		}
	}

	public static Bitmap getBitmapFromLocalCache(String path) {
		Bitmap bitmap = null;
		String state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			return null;
		}

		bitmap = BitmapFactory.decodeFile(path);

		return bitmap;
	}

	public void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}

	public Bitmap getBitmapFromWeb(String url, String cookie) {
		final HttpClient client = HttpManager.sClient;
				
		final HttpGet getRequest = new HttpGet(url);
		if (cookie != null) {
			getRequest.setHeader("cookie", cookie);
		}

		try {
			HttpResponse response = client.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Logger.w("ImageDownloader", "Error " + statusCode
						+ " while retrieving bitmap from " + url);
				return null;
			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				OutputStream outputStream = null;
				try {
					inputStream = entity.getContent();
					final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
					outputStream = new BufferedOutputStream(dataStream,
							IO_BUFFER_SIZE);
					copy(inputStream, outputStream);
					outputStream.flush();

					final byte[] data = dataStream.toByteArray();
					final Bitmap bitmap = BitmapFactory.decodeByteArray(data,
							0, data.length);

					return bitmap;

				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					if (outputStream != null) {
						outputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (IOException e) {
			getRequest.abort();
			Logger.w(LOG_TAG, "I/O error while retrieving bitmap from " + url);
		} catch (IllegalStateException e) {
			getRequest.abort();
			Logger.w(LOG_TAG, "Incorrect URL: " + url);
		} catch (Exception e) {
			getRequest.abort();
			Logger.w(LOG_TAG, "Error while retrieving bitmap from " + url);
		} finally {
			/*if (client != null) {
				client.getConnectionManager().shutdown();
			}*/
		}
		return null;
	}

	public Bitmap getBitmap(String url) {
		Bitmap bitmap = null;

		String path = getCacheFilePath(url);
		Logger.d(LOG_TAG, "getBitmap:" + url);
		bitmap = getBitmapFromLocalCache(path);
		if (bitmap == null) {
			bitmap = getBitmapFromWeb(url, "");
			if (bitmap == null) {
				return null;
			}
			saveBitmapToLocalCache(path, bitmap);
		}

		return bitmap;
	}

	/**
	 * The actual AsyncTask that will asynchronously download the image.
	 */
	class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
		private String url;
		private final WeakReference<ImageView> imageViewReference;

		public BitmapDownloaderTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * Actual download method.
		 */
		@Override
		protected Bitmap doInBackground(String... params) {
			url = params[0];
			String cookie = params[1];
			Bitmap bitmap = null;

			String path = getCacheFilePath(url);
			bitmap = getBitmapFromLocalCache(path);
			if (bitmap == null) {
				bitmap = getBitmapFromWeb(url, cookie);
				if (bitmap == null) {
					return null;
				}
				saveBitmapToLocalCache(path, bitmap);
			}

			return bitmap;
		}

		/**
		 * Once the image is downloaded, associates it to the imageView
		 */
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

			// Add bitmap to cache
			if (bitmap != null) {
				synchronized (sHardBitmapCache) {
					sHardBitmapCache.put(url, bitmap);
				}
			}

			if (imageViewReference != null) {
				ImageView imageView = imageViewReference.get();
				BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}

	/**
	 * A fake Drawable that will be attached to the imageView while the download
	 * is in progress.
	 * 
	 * <p>
	 * Contains a reference to the actual download task, so that a download task
	 * can be stopped if a new binding is required, and makes sure that only the
	 * last started download process can bind its result, independently of the
	 * download finish order.
	 * </p>
	 */
	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

		public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
			//super(Color.BLACK);
			bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(
					bitmapDownloaderTask);
		}

		public BitmapDownloaderTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}
}
