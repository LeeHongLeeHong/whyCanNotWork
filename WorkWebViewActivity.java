package com.flyrish.errorbook.activity;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

import com.flyrish.errorbook.R;
import com.flyrish.errorbook.until.RGBLuminanceSource;
import com.flyrish.errorbook.until.SharedPreferencesHelper;
import com.flyrish.errorbook.view.MyApplication;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "SetJavaScriptEnabled", "HandlerLeak" })
public class WorkWebViewActivity extends Activity{
	public static final String TAG = "ClassGroupActivity";
	private TextView class_join, back_usercenter,title_Text;
	private WebView mWebView;
	private ImageView goback,goforward,refersh_web,stop_load;
	private LinearLayout zuoye_back;
	private Boolean dialogExist = false;
	private String[] codeListChars;
	private ProgressDialog mProgress;
	private static final int PARSE_BARCODE_SUC = 300;
	private static final int PARSE_BARCODE_FAIL = 303;
	private String photo_path;
	private Bitmap scanBitmap;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		MyApplication.getInstance().addToApplication(WorkWebViewActivity.this);
		setContentView(R.layout.activity_class_group);
		//调用ShareSDK
		ShareSDK.initSDK(this);
		init();
	}
	
	public void init(){
		class_join = (TextView) findViewById(R.id.class_join);
		back_usercenter = (TextView) findViewById(R.id.userCenter);
		goback = (ImageView) findViewById(R.id.goback);
		goforward = (ImageView) findViewById(R.id.goNext);
		refersh_web = (ImageView) findViewById(R.id.refersh_web);
		stop_load = (ImageView) findViewById(R.id.stop_load);
		mWebView = (WebView) findViewById(R.id.class_webview);
		zuoye_back = (LinearLayout) findViewById(R.id.zuoyeback);
		title_Text = (TextView) findViewById(R.id.zuoye_title);
		//设置是否支持JavaScript
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		//Html页面适配手机屏幕
		//mWebView.getSettings().setUseWideViewPort(true);
		//mWebView.getSettings().setLoadWithOverviewMode(true);
		//设置页面可缩放性
		//mWebView.getSettings().setBuiltInZoomControls(true);
		Intent intent = getIntent();
		String url = null;
		if(intent.getExtras() != null){
		Bundle bundle = intent.getExtras();
			url = bundle.getString("URL");
		}
		if (url == null || url.equals("")){
			//url = "http://192.168.10.205:8080/hybrid.html";
			mWebView.loadUrl("file:///android_asset/test.html");
			zuoye_back.setVisibility(View.GONE);
			//mWebView.loadUrl("http://192.168.10.87:8080/myscrapbook/user/UserClassGroupAction.a?token=111111");
		}else{
			mWebView.loadUrl(url);
		}
		mWebView.setWebViewClient(new WebViewClient(){
			@Override
			@SuppressLint("SetJavaScriptEnabled")
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.startsWith("http://")||url.startsWith("https://")){
				   view.loadUrl(url);
				}else if (url.startsWith("zhaicuoben://")){
				   String jsonStr = url.substring(13);
				   Log.e("TAG", "Gson---"+jsonStr);
				   Gson gson = new Gson();
				   Map<String, Object> jsonMap = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>() {}.getType());
				   webAction(jsonMap);
				}
				
				return true;
			}
			@Override
		    public void onPageFinished(WebView view, String url) {
		       super.onPageFinished(view, url);
		   	   String topRightDefJson = getWebValueById("topRightDef");
		   	   reloadTopRightDef(topRightDefJson);
		    }

		});
		
		mWebView.setWebChromeClient(new WebChromeClient() {  
            @Override  
            public void onReceivedTitle(WebView view, String title) {  
                super.onReceivedTitle(view, title);  
                reloadTitle(title);
            }  
        }); 
		
		mWebView.addJavascriptInterface(WorkWebViewActivity.this, "getWebValueById");
		
		class_join.setOnClickListener(ok1);
		back_usercenter.setOnClickListener(ok1);
		goback.setOnClickListener(ok1);
		goforward.setOnClickListener(ok1);
		refersh_web.setOnClickListener(ok1);
		stop_load.setOnClickListener(ok1);
	}
		
	/**
	 * 调用扫描二维码/条码
	 */
	private void method(){
		Intent intent = new Intent();
		intent.setClass(WorkWebViewActivity.this, ScanActivity.class);
		startActivityForResult(intent, 1);
	}
	
	private Boolean classJoinMode = false;
	private String topRightDefActionMode = null;
	private String jsActionName = null;
	private Map<String, String> preActionParam;
	private Map<String, String> shareParam;

	
	OnClickListener ok1 = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.class_join:
				Log.e("TAG","选中了！！");
				topRightDefAction();
				break;
			case R.id.userCenter:
				finish();
				break;
			case R.id.goback:
				mWebView.goBack();
				break;
			case R.id.goNext:
				mWebView.goForward();
				break;
			case R.id.refersh_web:
				mWebView.reload();
				break;
			case R.id.stop_load:
				mWebView.stopLoading();
				break;
			}
		}
	};
	
	private void topRightDefAction() {
		Log.e("TAG","嘻嘻嘻嘻---"+topRightDefActionMode);
		if (topRightDefActionMode!=null && !topRightDefActionMode.equals("")) {
			if (topRightDefActionMode.equals("js")) {
				preAction();
			} else if (topRightDefActionMode.equals("share")) {
				shareAPP();
				
			} else if (topRightDefActionMode.equals("scan")) {
				classJoinMode = true;
				method();
			}
		}
	}
	
	private void preAction() {
		if (preActionParam.size()!=0) {
			if (preActionParam.get("action").equals("selectZCB")) {
				choseZCB();
			} else if (preActionParam.get("action").equals("")) {
				
			}
		} else {
			mWebView.loadUrl("javascript:"+jsActionName+"();");
		}
	}
	
	public void choseZCB() {
		SharedPreferencesHelper sph = SharedPreferencesHelper.instance(this, "USER_ID");
		String userId = sph.getValue("userid");
		//是否登录
		SharedPreferencesHelper loginSPH=SharedPreferencesHelper.instance(this,"isLogin");
		String isLogin = loginSPH.getValue("isLogin");
		Intent intent = new Intent(this,ZCBListActivity.class);
		intent.putExtra("isLogin", isLogin);
		intent.putExtra("userId", userId);
		startActivityForResult(intent, 111);
	}
	
	/**
	 * 创建日期及时间选择对话框
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case 1:
			dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.choose_method)
					.setItems(R.array.header_choose, mHeaderListener).create();
			dialog.setCanceledOnTouchOutside(false);
			break;
		case 100:
			dialog = new AlertDialog.Builder(this)
			        .setTitle("扫描到多个码").setItems(codeListChars, codeListListener).create();
			dialog.setCanceledOnTouchOutside(false);
			break;
		}
		return dialog;
	}
	
	/***
	 * 头像Dialog Listener
	 */
	private DialogInterface.OnClickListener mHeaderListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			dialogExist = false;
			switch (which) {
			case 0: // 拍照
				method();
				break;
			case 1: // 相册
				Intent intent = new Intent(Intent.ACTION_PICK, null);
				intent.setDataAndType(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
				startActivityForResult(intent, 2);
				break;
			default:
				break;
			}
		}
	};
	
	private DialogInterface.OnClickListener codeListListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			dialogExist = false;
			if (which!=codeListChars.length-1) {
				String code = String.valueOf(codeListChars[which]);
			    processScanCode(code);
			}
		}
	};
	
	private Handler mScanHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mProgress.dismiss();
			switch (msg.what) {
			case PARSE_BARCODE_SUC:
				Log.i("TAG", "ZXing扫描结果---"+(String)msg.obj);
				processScanCode((String)msg.obj);
				break;
			case PARSE_BARCODE_FAIL:
				Toast.makeText(WorkWebViewActivity.this, (String)msg.obj, Toast.LENGTH_LONG).show();
				break;
			}
		}
		
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("TAG", "回调");
		super.onActivityResult(requestCode, resultCode, data);
		Log.i("TAG", "向下执行");

		// 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
		
		switch (requestCode) {
		case 1:
			if (resultCode == Activity.RESULT_OK) {
				Log.i("TAG", "RESULT_OK");
				Bundle result = data.getExtras();
				//SymbolSet resultSet = (SymbolSet)result.getSerializable("SymbolSet");
				ArrayList<String> codeList = new ArrayList<String>(); 
				codeList = result.getStringArrayList("SymbolSet");
				Log.i("TAG", "结果数目---"+codeList.size());
				//for (Symbol sym : resultSet) {
		        //    codeList.add(sym.getData());
		        //}
				this.processScanCodeList(codeList);
			}
			break;

		case 2:// 相册
			if (resultCode == Activity.RESULT_OK) {
				try {
					// 获得图片的uri
					/*Uri originalUri = data.getData();
					ContentResolver resolver = getContentResolver();
					byte[] mContent = readStream(resolver.openInputStream(Uri  
	                        .parse(originalUri.toString()))); 
					
				    this.scanImages(mContent);
					*/
					Cursor cursor = getContentResolver().query(data.getData(), null, null, null, null);
					if (cursor.moveToFirst()) {
						photo_path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
					}
					cursor.close();
					
					mProgress = new ProgressDialog(WorkWebViewActivity.this);
					mProgress.setMessage("正在扫描，请稍候");
					mProgress.setCancelable(false);
					mProgress.show();
					
					new Thread(new Runnable() {
						@Override
						public void run() {
							Result result = scanningImage(photo_path, 0);
							if (result != null) {
								Message m = mScanHandler.obtainMessage();
								m.what = PARSE_BARCODE_SUC;
								m.obj = result.getText();
								mScanHandler.sendMessage(m);
							} else {
								result = scanningImage(photo_path, 90);
								if (result != null) {
									Message m = mScanHandler.obtainMessage();
									m.what = PARSE_BARCODE_SUC;
									m.obj = result.getText();
									mScanHandler.sendMessage(m);
								} else {
									result = scanningImage(photo_path, 45);
									if (result != null) {
										Message m = mScanHandler.obtainMessage();
										m.what = PARSE_BARCODE_SUC;
										m.obj = result.getText();
										mScanHandler.sendMessage(m);
									} else {
										Message m = mScanHandler.obtainMessage();
										m.what = PARSE_BARCODE_FAIL;
										m.obj = "未扫描到有效内容";
										mScanHandler.sendMessage(m);
									}
								}
								/*Message m = mScanHandler.obtainMessage();
								m.what = PARSE_BARCODE_FAIL;
								m.obj = "未扫描到有效内容";
								mScanHandler.sendMessage(m);*/
							}
						}
					}).start();
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
			break;
		case 111:
			if (resultCode == Activity.RESULT_OK) {
				int id = data.getIntExtra("zcbId", 0);
				String js = "javascript:document.getElementById('"+preActionParam.get("objID")+"').value='"+id+"';";
				mWebView.loadUrl(js);
				mWebView.loadUrl("javascript:"+jsActionName+"();");
			}
			break;
		}
	}
	
	public Result scanningImage(String path,int rotate) {
		if(TextUtils.isEmpty(path)){
			return null;
		}
		Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
		hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); 

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true; 
		scanBitmap = BitmapFactory.decodeFile(path, options);
		options.inJustDecodeBounds = false; 
		int sampleSize = (int) (options.outHeight / (float) 200);
		if (sampleSize <= 0){
			sampleSize = 1;
		}
		options.inSampleSize = sampleSize;
		scanBitmap = BitmapFactory.decodeFile(path, options);
		if (rotate!=0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(rotate);
			Bitmap roBm = Bitmap.createBitmap(scanBitmap, 0, 0,  scanBitmap.getWidth(), scanBitmap.getHeight(), matrix, true);
			scanBitmap = roBm;
		}
		RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);
		BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
		QRCodeReader reader = new QRCodeReader();
		try {
			return reader.decode(bitmap1, hints);
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (ChecksumException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void webAction(Map<String, Object> jsonMap) {
		String go = (String) jsonMap.get("go");
		if (go.equals("web")) {
			openWeb(jsonMap);
		} else if (go.equals("scan")) {
			doScan(jsonMap);
		}else if (go.equals("webNoSegue")) {
			loadWeb(jsonMap);
		}else if (go.equals("uploadAnswerImages")) {
			uploadAnswerImages(jsonMap);
		}else if (go.equals("post")) {
			doPost();
		} else if (go.equals("dateSelect")) {
			dateSelect();
		} else {

		}
		/*  Cannot switch on a value of type String for source level below 1.7.  
		switch ((String)jsonMap.get("go")){
		case "web":
		   openWeb(jsonMap);
		case "scan":
			doScan(jsonMap);
		case "post":
			doPost();
		case "dateSelect":
			dateSelect();
		} */
	}
	
	private void openWeb(Map<String, Object> map) {
		String nextURL = (String)map.get("url");
		String tokenKey = (String)map.get("tokenKey");
		String finalTargetURL = null;
		
		if (nextURL.startsWith("http://")){
			if (tokenKey!=null) {
				finalTargetURL = appendTokenForURL(nextURL, tokenKey);
			}
		}else{
			String currentURL = mWebView.getUrl();
			finalTargetURL = composeURL(nextURL, currentURL);
			if (tokenKey!=null) {
				finalTargetURL = appendTokenForURL(finalTargetURL, tokenKey);
			}
		}
		
		if (finalTargetURL != null && !finalTargetURL.equals("")) {
			Intent intent = new Intent();
			intent.setClass(WorkWebViewActivity.this, WorkWebViewActivity.class);
			Bundle bundle = new Bundle();
        	bundle.putString("URL", finalTargetURL);
        	intent.putExtras(bundle);
			startActivity(intent);
		}
	}
	
	private void loadWeb(Map<String, Object> map) {
		String nextURL = (String)map.get("url");
		String tokenKey = (String)map.get("tokenKey");
		String finalTargetURL = null;
		
		if (nextURL.startsWith("http://")){
			if (tokenKey!=null) {
				finalTargetURL = appendTokenForURL(nextURL, tokenKey);
			}
		}else{
			String currentURL = mWebView.getUrl();
			finalTargetURL = composeURL(nextURL, currentURL);
			if (tokenKey!=null) {
				finalTargetURL = appendTokenForURL(finalTargetURL, tokenKey);
			}
		}
		mWebView.loadUrl(finalTargetURL);
	}

	
	private String composeURL(String targetURL, String baseURL) {
	    String pathComponents[] = baseURL.split("/");
	    if (targetURL.startsWith("/")) {
	       String newURL = pathComponents[0]+"//"+pathComponents[2]+targetURL;
	       return newURL;
	    }else {
	       String newURL = pathComponents[0]+"//"+pathComponents[2];
	       for (int i=3; i<pathComponents.length; i++) {
	    	   newURL = newURL+"/"+pathComponents[i];
	       }
	       newURL = newURL+"/"+targetURL;
	       return newURL;
	    }
	}
	
	private String appendTokenForURL(String url, String tokenKey) {
		SharedPreferencesHelper tokenspfh = SharedPreferencesHelper.instance(this,"TOKEN");
		String token = tokenspfh.getValue("token");
		if (token==null)
			token = "dfsjfksjlf";
		String finalURL = url;
		String str1 = "?"+token.toLowerCase()+"=";
		int idx1 = url.toLowerCase().indexOf(str1);
		if (idx1 > -1){
			
		}else{
			String str2 = "&"+tokenKey.toLowerCase()+"=";
			int idx2 = url.toLowerCase().indexOf(str2);
			if (idx2 > -1){
				
			}else{
				int markIdx = url.indexOf("?");
				if (markIdx > -1){
					finalURL = url+"&"+tokenKey+"="+token;
				}else{
					finalURL = url+"?"+tokenKey+"="+token;
				}
			}
		}
		return finalURL;
	}
	
	private String objID;
	private String func;
	
	private void doScan(Map<String, Object> map) {
		objID = (String)map.get("objID");
		func = (String)map.get("func");
		if (dialogExist==false) {
			onCreateDialog(1).show();	
			dialogExist = true;
		}
	}
	
	private void processScanCodeList(ArrayList<String> codeList) {
		if (codeList.size()>1) {
			codeListChars = null;
			codeList.add("取消");
			codeListChars = (String[])codeList.toArray(new String[0]);
			if (dialogExist==false) {
				onCreateDialog(100).show();	
				dialogExist = true;
			}
		}
		else if (codeList.size()==1) {
			Log.i("TAG", "只有一个结果");
			this.processScanCode(codeList.get(0));
		}
	}
	
	private void processScanCode(String code) {
		Log.i("TAG", "结果为"+code);
		if (objID!=null) {
			Log.i("TAG", "通过objID："+objID);
			String js = "javascript:document.getElementById('"+objID+"').value='"+code+"';";
			mWebView.loadUrl(js);
			js = "javascript:document.getElementById('"+objID+"').innerText='"+code+"';";
			mWebView.loadUrl(js);
		}
		
		if (func!=null) {
			Log.i("TAG", "通过func："+func);
			String js = "javascript:"+func+"('"+code+"');";
			mWebView.loadUrl(js);
		}
		
		if (classJoinMode==true && ((code.startsWith("http://")) || (code.startsWith("https://")))) {
			Gson gson = new Gson();
			String jsonStr = "{\"go\":\"webNoSegue\",\"url\":\""+code+"\",\"tokenKey\":\"token\"}";
			Map<String, Object> jsonMap = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>() {}.getType());
			webAction(jsonMap); 
			classJoinMode = false;
		}
	}
	
	private void doPost() {
		
	}
	
	private void dateSelect() {
		
	}
	
	private String getWebValueById(String id) {
		String js = "javascript:getWebValueById.receiveValueFrom(document.getElementById('"+id+"').value);";
		mWebView.loadUrl(js);
		
		while(receivedValue==null && valueReceived == false ) {
		}
		valueReceived = false;
		return receivedValue; 
	}
	
	private String receivedValue = null;
	private Boolean valueReceived = false;
	
	public void receiveValueFrom(String id) {
		if (id != null && !id.equals("")) {
			receivedValue = id;
		}else {
			receivedValue = null;
			valueReceived = true;
		}
	}

	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Intent intent = getIntent();
		String work_url = null;
		if(intent.getExtras() != null){
		Bundle bundle = intent.getExtras();
			work_url = bundle.getString("URL");
		}
		if(keyCode == KeyEvent.KEYCODE_BACK ){
			if(work_url != null && !work_url.equals("")){
				finish();
			}else{
				dialog();
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	protected void dialog() {
		AlertDialog.Builder builder = new Builder(WorkWebViewActivity.this);
		builder.setMessage("确认是否退出");
		builder.setTitle("提示");
		builder.setPositiveButton("确认",
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						//android.os.Process.killProcess(android.os.Process.myPid());
						MyApplication.getInstance().onTerminate();
					}
				});
		builder.setNegativeButton("取消",
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();
	}
	
	@SuppressWarnings("unchecked")
	private void reloadTopRightDef(String topRightDefJson) {
		Gson gson = new Gson();
		if (!topRightDefJson.startsWith("{"))
			return;
		Map<String, Object> jsonMap = gson.fromJson(topRightDefJson, new TypeToken<Map<String, Object>>() {}.getType());
		if (jsonMap.get("title")==null)
			return;
		class_join.setText((CharSequence) jsonMap.get("title"));
		if (jsonMap.get("enabled")==null)
			class_join.setEnabled(true);
		else
			class_join.setEnabled((Double)jsonMap.get("enabled")==1);
		if (jsonMap.get("action")==null)
			return;
		topRightDefActionMode = (String) jsonMap.get("action");
		Log.e("TAG", "topRightDefActionMode---"+topRightDefActionMode);
		if (jsonMap.get("params")!=null)
			shareParam = (Map<String, String>) jsonMap.get("params");
		if (jsonMap.get("func")!=null)
			jsActionName = (String) jsonMap.get("func");
		if (jsonMap.get("preAction")!=null)
			preActionParam = (Map<String, String>) jsonMap.get("preAction");
	}
	
	public void reloadTitle(String title){
		if(title != null && !title.equals("")){
			title_Text.setText(title);
		}
	}
	
   private void uploadAnswerImages(Map<String, Object> map) {
		
	}
}
