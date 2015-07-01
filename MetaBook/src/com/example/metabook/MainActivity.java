/** 
 * Book Barcode Reader - MetaBook
 * @author Swetha Ambati
 */

package com.example.metabook;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.net.URL;
import java.net.URLConnection;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView titleName, authorName, publishedDate, pageCount,bookCategory, bookRatings, bookDescription;
	private ImageView bookCoverImg;
	private LinearLayout starRatingsLayout;
	private ImageView[] starRatings;
	private Bitmap thumbnailImg;
	public static final int READER_RESULT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// scan button initiating the zxing scan intent
		ImageButton scanBtn = (ImageButton) findViewById(R.id.button_scan);
		scanBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				titleName.setText("");
				authorName.setText("");
				bookDescription.setText("");
				publishedDate.setText("");
				pageCount.setText("");
				bookCategory.setText("");
				starRatingsLayout.removeAllViews();
				bookRatings.setText("");
				bookCoverImg.setImageBitmap(null);
				// Intent for zxing scan
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "ONE_D_MODE");
				intent.putExtra("SCAN_RESULT_FORMAT", "ISBN");
				startActivityForResult(intent, READER_RESULT);
			}
		});

		titleName = (TextView) findViewById(R.id.book_titleName);
		authorName = (TextView) findViewById(R.id.book_authorName);
		bookDescription = (TextView) findViewById(R.id.book_description);
		publishedDate = (TextView) findViewById(R.id.book_publishedDate);
		pageCount = (TextView) findViewById(R.id.book_pageCount);
		bookCategory = (TextView) findViewById(R.id.book_category);
		starRatingsLayout = (LinearLayout) findViewById(R.id.starRatings_layout);
		bookRatings = (TextView) findViewById(R.id.book_ratings);
		bookCoverImg = (ImageView) findViewById(R.id.book_coverImg);
		
		starRatings = new ImageView[5];
		for (int star = 0; star < starRatings.length; star++) {
			starRatings[star] = new ImageView(this);
		}
		// retrieving the book info value every time the orientation of the mobile changes
		if (savedInstanceState != null) {
			titleName.setText(savedInstanceState.getString("title"));
			authorName.setText(savedInstanceState.getString("author"));
			thumbnailImg = (Bitmap) savedInstanceState.getParcelable("coverImg");
			bookCoverImg.setImageBitmap(thumbnailImg);
			publishedDate.setText(savedInstanceState.getString("publishedDate"));
			pageCount.setText(savedInstanceState.getString("pages"));
			bookCategory.setText(savedInstanceState.getString("category"));
			bookRatings.setText(savedInstanceState.getString("ratings"));
			int numStars = savedInstanceState.getInt("stars");
			for (int s = 0; s < numStars; s++) {
				starRatings[s].setImageResource(R.drawable.star);
				starRatingsLayout.addView(starRatings[s]);
			}
			starRatingsLayout.setTag(numStars);
			
			bookDescription.setText(savedInstanceState.getString("description"));
		}
	}
	// @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == READER_RESULT) {
			if (resultCode == RESULT_OK) {
				// getting ISBN value from the scan result
				String contents = intent.getStringExtra("SCAN_RESULT");
				Log.i("SCAN", contents);
				// searching for book information using Google Books API and the ISBN number
				String bookSearch = "https://www.googleapis.com/books/v1/volumes?"
						+ "q=isbn:"
						+ contents
						+ "&key=AIzaSyBNnHioUGvnekISk7ycWjSJWFpvS7CM8po";
				Log.i("value", bookSearch);
				new BookInfo().execute(bookSearch);
			}
		}
	}
	// Gets book information 
	private class BookInfo extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... bookURLs) {
			StringBuilder bookBuilder = new StringBuilder();
			for (String bookSearchURL : bookURLs) {
				HttpClient bookClient = new DefaultHttpClient();
				try {
					HttpGet bookInfoGet = new HttpGet(bookSearchURL);
					HttpResponse bookResponse = bookClient.execute(bookInfoGet);
					StatusLine bookSearchStatus = bookResponse.getStatusLine();
					if (bookSearchStatus.getStatusCode() == 200) {
						HttpEntity bookEntity = bookResponse.getEntity();
						InputStream bookContent = bookEntity.getContent();
						InputStreamReader bookInput = new InputStreamReader(
								bookContent);
						BufferedReader bookReader = new BufferedReader(
								bookInput);
						String lineIn;
						while ((lineIn = bookReader.readLine()) != null) {
							bookBuilder.append(lineIn);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return bookBuilder.toString();
		}
		//parsing JSON
		protected void onPostExecute(String result) {
			try {
				JSONObject resultObject = new JSONObject(result);
				JSONArray bookArray = resultObject.getJSONArray("items");
				JSONObject bookObject = bookArray.getJSONObject(0);
				JSONObject volumeObject = bookObject
						.getJSONObject("volumeInfo");
				//book title
				try {
					titleName.setText("" + volumeObject.getString("title"));
				} catch (JSONException jse) {
					titleName.setText("");
					jse.printStackTrace();
				}
				// book author
				StringBuilder authorBuild = new StringBuilder("");
				try {
					JSONArray authorArray = volumeObject
							.getJSONArray("authors");
					for (int author = 0; author < authorArray.length(); author++) {
						if (author > 0)
							authorBuild.append(", ");
						authorBuild.append(authorArray.getString(author));
					}
					authorName.setText("by " + authorBuild.toString());
				} catch (JSONException jse) {
					authorName.setText("");
					jse.printStackTrace();
				}
				// book cover image
				try {
					JSONObject imageInfo = volumeObject
							.getJSONObject("imageLinks");
					new GetBookCoverImg().execute(imageInfo
							.getString("smallThumbnail"));
				} catch (JSONException jse) {
					bookCoverImg.setImageBitmap(null);
					jse.printStackTrace();
				}
				// book published date
				try {
					publishedDate.setText("Published on "
							+ volumeObject.getString("publishedDate"));
				} catch (JSONException jse) {
					publishedDate.setText("");
					jse.printStackTrace();
				}
				// book pages count
				try {
					pageCount.setText(volumeObject.getString("pageCount")
							+ " Pages ");
				} catch (JSONException jse) {
					pageCount.setText("");
					jse.printStackTrace();
				}
				//book category	
				StringBuilder categoryBuild = new StringBuilder("");
				try {

					JSONArray categoryArray = volumeObject
							.getJSONArray("categories");
					for (int c = 0; c < categoryArray.length(); c++) {
						if (c > 0)
							categoryBuild.append(", ");
						categoryBuild.append(categoryArray.getString(c));
					}

					bookCategory
							.setText("Category " + categoryBuild.toString());
				} catch (JSONException jse) {
					bookCategory.setText("");
					jse.printStackTrace();
				}
				
				// book ratings count
				try {
					bookRatings.setText("("
							+ volumeObject.getString("ratingsCount")
							+ ") ratings");
				} catch (JSONException jse) {
					bookRatings.setText("");
					jse.printStackTrace();
				}
				
				// book star ratings
				try {
					double decNumStars = Double.parseDouble(volumeObject
							.getString("averageRating"));
					int numStars = (int) decNumStars;
					starRatingsLayout.setTag(numStars);
					starRatingsLayout.removeAllViews();
					for (int s = 0; s < numStars; s++) {
						starRatings[s].setImageResource(R.drawable.star);
						starRatingsLayout.addView(starRatings[s]);
					}
				} catch (JSONException jse) {
					starRatingsLayout.removeAllViews();
					jse.printStackTrace();
				}
				// book description
				try {
					bookDescription.setText("Description: "
							+ volumeObject.getString("description"));
				} catch (JSONException jse) {
					bookDescription.setText("");
					jse.printStackTrace();
				}
	
			} catch (Exception e) {
				// no result
				e.printStackTrace();

				Toast toast = Toast.makeText(getApplicationContext(),
						"Not an ISBN format", Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);

				toast.show();

			}
		}
	}

	// Gets book cover image
	private class GetBookCoverImg extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... thumbnailURLs) {
			try {
				// downloading book cover image by parsing thumbnail image
				URL thumbnailURL = new URL(thumbnailURLs[0]);
				URLConnection thumbnailConn = thumbnailURL.openConnection();
				thumbnailConn.connect();
				InputStream thumbnailIn = thumbnailConn.getInputStream();
				BufferedInputStream thumbnailBuff = new BufferedInputStream(thumbnailIn);
				thumbnailImg = BitmapFactory.decodeStream(thumbnailBuff);

				thumbnailBuff.close();
				thumbnailIn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
		//displaying the book cover image
		protected void onPostExecute(String result) {
			bookCoverImg.setImageBitmap(thumbnailImg);
		}
	}

	// // saving the book info values before the activity gets destroyed
	protected void onSaveInstanceState(Bundle savedBundle) {
		savedBundle.putString("title", "" + titleName.getText());
		savedBundle.putString("author", "" + authorName.getText());
		savedBundle.putParcelable("coverImg", thumbnailImg);
		savedBundle.putString("publishedDate", "" + publishedDate.getText());
		savedBundle.putString("pages", "" + pageCount.getText());
		savedBundle.putString("category", "" + bookCategory.getText());
		savedBundle.putString("ratings", "" + bookRatings.getText());
		if (starRatingsLayout.getTag() != null)
		{
			savedBundle.putInt("stars", Integer.parseInt(starRatingsLayout.getTag().toString()));
		}
		savedBundle.putString("description", "" + bookDescription.getText());

	}
}
