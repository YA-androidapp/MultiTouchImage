package jp.gr.java_conf.ya.multitouchimage; // Copyright (c) 2013-2016 YA <ya.androidapp@gmail.com> All rights reserved.

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

// http://blog.livedoor.jp/sylc/archives/1470690.html

public class MultiTouchImageActivity extends Activity {
	private Handler handler = new Handler();
	private Map<String, Bitmap> icons = new HashMap<String, Bitmap>();
	ScalableView scalableView1, scalableView2, scalableView3;
	TextView textView1, textView2, textView3, textView4;
	Button button1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		scalableView1 = (ScalableView) this.findViewById(R.id.imageView1);
		scalableView2 = (ScalableView) this.findViewById(R.id.imageView2);
		scalableView3 = (ScalableView) this.findViewById(R.id.imageView3);
		textView1 = (TextView) this.findViewById(R.id.textView1);
		textView2 = (TextView) this.findViewById(R.id.textView2);
		textView3 = (TextView) this.findViewById(R.id.textView3);
		textView4 = (TextView) this.findViewById(R.id.textView4);
		button1 = (Button) this.findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				Thread thread3 = new Thread(new Runnable() {
					public void run() {
						ArrayList<String> ITEMS = new ArrayList<String>();

						String URL = "http://weather.livedoor.com/forecast/rss/primary_area.xml";
						String text = "";
						try {
							text = new String(http2data(URL));
						} catch (Exception e) {
							Log.v("Weather", "e: " + e.toString() + e.getMessage());
						}
						try {
							DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
							DocumentBuilder docbuilder = dbfactory.newDocumentBuilder();
							Document doc = docbuilder.parse(new ByteArrayInputStream(text.getBytes("UTF-8")));
							Element root = doc.getDocumentElement();
							NodeList channel = root.getElementsByTagName("channel");
							NodeList source = ((Element) channel.item(0)).getElementsByTagName("ldWeather:source");
							NodeList pref = ((Element) source.item(0)).getElementsByTagName("pref");
							for (int i = 0; i < pref.getLength(); i++) {
								Node child = pref.item(i);
								if (child instanceof Element) {
									Element childElement = (Element) child;
									Log.v("Weather", "Pref: " + childElement.getAttribute("title"));
									ITEMS.add(childElement.getAttribute("title"));

									NodeList city = childElement.getElementsByTagName("city");
									for (int j = 0; j < city.getLength(); j++) {
										Node child2 = city.item(j);
										if (child2 instanceof Element) {
											Element childElement2 = (Element) child2;
											Log.v("Weather", "City: " + childElement2.getAttribute("title")
													+ "=>" + childElement2.getAttribute("id"));
											ITEMS.add(childElement2.getAttribute("title")
													+ "=>" + childElement2.getAttribute("id"));
										}

									}
								}

							}
						} catch (Exception e) {
							Log.v("Weather", "e: " + e.toString() + e.getMessage());
						}

						final String[] ITEM = ITEMS.toArray(new String[ITEMS.size()]);
						runOnUiThread(new Runnable() {
							public void run() {
								new AlertDialog.Builder(MultiTouchImageActivity.this)
										.setTitle("AlertDialog")
										.setItems(ITEM,
												new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int which) {
														if (ITEM[which].contains("=>")) {
															getWeather(ITEM[which].substring(ITEM[which].length() - 6));
															try {
																saveStr(ITEM[which].substring(ITEM[which]
																		.length() - 6));
															} catch (IOException e) {
																Log.v("MTIA", "e: " + e.toString());
															}
														} else {
															getWeather(ITEM[which + 1].substring(ITEM[which + 1]
																	.length() - 6));
															try {
																saveStr(ITEM[which + 1].substring(ITEM[which + 1]
																		.length() - 6));
															} catch (IOException e) {
																Log.v("MTIA", "e: " + e.toString());
															}
														}
													}
												}).create().show();
							}
						});
					}
				});
				thread3.start();
			}
		});

		getWeather("120010");
	}

	private void saveStr(String str) throws IOException {
		File file = new File(Environment.getExternalStorageDirectory().getPath() + "/placeId.txt");
		file.getParentFile().mkdir();

		FileOutputStream fos = new FileOutputStream(file, false);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);

		bw.write(str);
		bw.flush();
		bw.close();
	}

	public void getWeather(final String placeId) {
		Log.v("Weather", "placeId: " + placeId);
		Thread thread2 = new Thread(new Runnable() {
			public void run() {
				String URL = "http://weather.livedoor.com/forecast/webservice/json/v1?city=" + placeId;
				String text = "";
				try {
					text = new String(http2data(URL));
				} catch (Exception e) {
				}
				JSONObject rootObject = null;
				try {
					rootObject = new JSONObject(text);
				} catch (JSONException e) {
				}
				String city = "";
				try {
					city = rootObject.getJSONObject("location").getString("city");

					Log.d("JSON", "city: " + city);
				} catch (JSONException e) {
				}
				JSONArray foreArray = null;
				try {
					foreArray = rootObject.getJSONArray("forecasts");
				} catch (JSONException e) {
				}

				String[] result = new String[foreArray.length()];
				String[] iconurl = new String[foreArray.length()];

				try {
					for (int i = 0; i < foreArray.length(); i++) {
						JSONObject foreObject = foreArray.getJSONObject(i);
						result[i] = foreObject.getString("dateLabel")
								+ ": " + foreObject.getString("telop");
						try {
							result[i] += " min: "
									+ foreObject.getJSONObject("temperature").getJSONObject("min").getString("celsius");
						} catch (JSONException e1) {
						}
						try {
							result[i] += " max: "
									+ foreObject.getJSONObject("temperature").getJSONObject("max").getString("celsius");
						} catch (JSONException e1) {
						}
						try {
							Log.d("JSON", "iconurl: "
									+ foreObject.getJSONObject("image").getString("url"));
							iconurl[i] = foreObject.getJSONObject("image").getString("url");
						} catch (JSONException e1) {
						}
					}
				} catch (JSONException e) {
				}

				final String ci = city;
				final String[] rs = result;
				final String[] ic = iconurl;
				handler.post(new Runnable() {
					public void run() {
						if (ci.equals("") == false)
							textView1.setText(ci);

						try {
							if (rs[0] != null) {
								if (rs[0].equals("") == false) {
									textView2.setText(rs[0]);
								} else {
									textView2.setVisibility(View.GONE);
								}
							} else {
								textView2.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							textView2.setVisibility(View.GONE);
						}
						try {
							if (rs[1] != null) {
								if (rs[1].equals("") == false) {
									textView3.setText(rs[1]);
								} else {
									textView3.setVisibility(View.GONE);
								}
							} else {
								textView3.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							textView3.setVisibility(View.GONE);
						}
						try {
							if (rs[2] != null) {
								if (rs[2].equals("") == false) {
									textView4.setText(rs[2]);
								} else {
									textView4.setVisibility(View.GONE);
								}
							} else {
								textView4.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							textView4.setVisibility(View.GONE);
						}

						try {
							if (ic[0] != null) {
								if (ic[0].equals("") == false) {
									readIcon(scalableView1, ic[0]);
								} else {
									scalableView1.setVisibility(View.GONE);
								}
							} else {
								scalableView1.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							scalableView1.setVisibility(View.GONE);
						}
						try {
							if (ic[1] != null) {
								if (ic[1].equals("") == false) {
									readIcon(scalableView2, ic[1]);
								} else {
									scalableView2.setVisibility(View.GONE);
								}
							} else {
								scalableView2.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							scalableView2.setVisibility(View.GONE);
						}
						try {
							if (ic[2] != null) {
								if (ic[2].equals("") == false) {
									readIcon(scalableView3, ic[2]);
								} else {
									scalableView3.setVisibility(View.GONE);
								}
							} else {
								scalableView3.setVisibility(View.GONE);
							}
						} catch (Exception e) {
							scalableView3.setVisibility(View.GONE);
						}
					}
				});
			}
		});
		thread2.start();

	}

	//HTTP通信
	public static byte[] http2data(String path) throws Exception {
		byte[] w = new byte[1024];
		HttpURLConnection c = null;
		InputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			//HTTP接続のオープン
			URL url = new URL(path);
			c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			c.connect();
			in = c.getInputStream();

			//バイト配列の読み込み
			out = new ByteArrayOutputStream();
			while (true) {
				int size = in.read(w);
				if (size <= 0)
					break;
				out.write(w, 0, size);
			}
			out.close();

			//HTTP接続のクローズ
			in.close();
			c.disconnect();
			return out.toByteArray();
		} catch (Exception e) {
			try {
				if (c != null)
					c.disconnect();
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (Exception e2) {
				Log.v("Weather", "e2: " + e2.toString() + e2.getMessage());
			}
			Log.v("Weather", "e: " + e.toString() + e.getMessage());
			throw e;
		}
	}

	// アイコンの読み込み
	public void readIcon(final ScalableView scalableView, final String url) {
		// キャッシュ内アイコンの取得
		if (icons.containsKey(url)) {
			handler.post(new Runnable() {
				public void run() {
					scalableView.setImageBitmap(icons.get(url));
				}
			});
			return;
		}
		// ネット上アイコンの取得
		scalableView.setImageBitmap(null);
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					InputStream input = (new URL(url)).openStream();
					final Bitmap icon = BitmapFactory.decodeStream(input);
					if (icon != null) {
						icons.put(url, icon);
						handler.post(new Runnable() {
							public void run() {
								scalableView.setImageBitmap(icon);
							}
						});
					}
				} catch (Exception e) {
				}
			}
		});
		thread.start();
	}

}