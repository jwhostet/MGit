package me.sheimi.sgit.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.CodeGuesser;
import me.sheimi.android.utils.FsUtils;
import me.sheimi.sgit.R;
import me.sheimi.sgit.dialogs.ChooseLanguageDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ViewFileActivity extends SheimiFragmentActivity {

    public static String TAG_FILE_NAME = "file_name";
    private WebView mFileContent;
    private static final String JS_INF = "CodeLoader";
    private ProgressBar mLoading;
    private File mFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_file);
        setupActionBar();
        mFileContent = (WebView) findViewById(R.id.fileContent);
        mLoading = (ProgressBar) findViewById(R.id.loading);

        Bundle extras = getIntent().getExtras();
        String fileName = extras.getString(TAG_FILE_NAME);
        mFile = new File(fileName);
        setTitle(mFile.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFileContent();
    }

    private void loadFileContent() {
        mFileContent.loadDataWithBaseURL("file:///android_asset/", HTML_TMPL,
                "text/html", "utf-8", null);
        mFileContent.addJavascriptInterface(new CodeLoader(), JS_INF);
        WebSettings webSettings = mFileContent.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mFileContent.setWebChromeClient(new WebChromeClient() {
            public void onConsoleMessage(String message, int lineNumber,
                    String sourceID) {
                Log.d("MyApplication", message + " -- From line " + lineNumber
                        + " of " + sourceID);
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.view_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_edit:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_EDIT);
                Uri uri = Uri.fromFile(mFile);
                String mimeType = FsUtils.getMimeType(uri.toString());
                intent.setDataAndType(uri, mimeType);
                try {
                    startActivity(intent);
                    forwardTransition();
                } catch (ActivityNotFoundException e) {
                    showToastMessage(R.string.error_no_edit_app);
                }
                return true;
            case R.id.action_choose_language:
                ChooseLanguageDialog cld = new ChooseLanguageDialog();
                cld.show(getSupportFragmentManager(), "choose language");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setLanguage(String lang) {
        String js = String.format("setLanguage('%s')", lang);
        mFileContent.loadUrl(CodeGuesser.wrapUrlScript(js));
    }

    private class CodeLoader {

        private String mCode;
        private int mCodeLines;

        @JavascriptInterface
        public String getCode() {
            return mCode;
        }

        @JavascriptInterface
        public int getLineNumber() {
            return mCodeLines;
        }

        @JavascriptInterface
        public String getLanguage() {
            return CodeGuesser.guessCodeType(mFile.getName());
        }

        @JavascriptInterface()
        public void loadCode() {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(
                                mFile));
                        StringBuffer sb = new StringBuffer();
                        String line = br.readLine();
                        mCodeLines = 0;
                        while (null != line) {
                            mCodeLines++;
                            sb.append(line);
                            sb.append('\n');
                            line = br.readLine();
                        }
                        mCode = sb.toString();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLoading.setVisibility(View.INVISIBLE);
                            mFileContent.loadUrl(CodeGuesser
                                    .wrapUrlScript("notifyFileLoaded();"));
                        }
                    });
                }
            });
            thread.start();
        }

    }

    private static final String HTML_TMPL = "<!doctype html>"
            + "<head>"
            + " <script src=\"js/jquery.js\"></script>"
            + " <script src=\"js/highlight.pack.js\"></script>"
            + " <script src=\"js/local_viewfile.js\"></script>"
            + " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/rainbow.css\" />"
            + " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/local_viewfile.css\" />"
            + "</head><body><table>"
            + "<tbody><tr><td class=\"line_number_td\">"
            + "<pre class=\"line_numbers\"></pre>"
            + "</td><td class=\"codes_td\"><pre class=\"codes\"><code></code></pre>"
            + "</td></tr></tbody></table></body>";

}
