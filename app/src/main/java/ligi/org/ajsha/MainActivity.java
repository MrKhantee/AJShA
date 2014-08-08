package ligi.org.ajsha;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.ligi.axt.AXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;

import bsh.ClassIdentifier;
import bsh.EvalError;
import bsh.Interpreter;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends Activity {

    public static final String CODE_KEY = "code";
    private Interpreter interpreter;

    @InjectView(R.id.exception_out)
    TextView exceptionOut;

    @InjectView(R.id.obj_out)
    TextView objOut;


    @InjectView(R.id.out_stream)
    TextView streamedOutTV;

    @InjectView(R.id.obj_tostring)
    TextView toStringTV;

    @InjectView(R.id.codeInput)
    EditText codeEditText;

    @InjectView(R.id.linearLayout)
    LinearLayout linearLayout;

    private String streamedOutString;

    @OnClick(R.id.execCodeButton)
    void execCodeonClick() {
        try {
            streamedOutString = "";
            streamedOutTV.setText(streamedOutString);

            final Object evaledObject = interpreter.eval("import android.content.*;import android.widget.*;import android.os.*;import org.ligi.axt.*;" + codeEditText.getText().toString());

            exceptionOut.setText("");
            if (evaledObject == null) {
                objOut.setText("VOID");
                toStringTV.setText("");
            } else {

                final Class evalClass;
                if (evaledObject instanceof ClassIdentifier) {
                    evalClass = ((ClassIdentifier) evaledObject).getTargetClass();
                } else {
                    evalClass = evaledObject.getClass();
                }

                final Spanned html = Html.fromHtml("<a href='" + getLinkForClass(evalClass) + "'>" + evalClass.toString() + "</a>");
                objOut.setText(html);
                objOut.setMovementMethod(LinkMovementMethod.getInstance());

                toStringTV.setText(evaledObject.toString());
            }
        } catch (EvalError evalError) {
            exceptionOut.setText("" + evalError);
            evalError.printStackTrace();
        }
    }

    private String getLinkForClass(Class inClass) {
        String link = inClass.getCanonicalName();
        link = link.replace(".", "/");
        return "http://developer.android.com/reference/" + link + ".html";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        interpreter = new Interpreter();

        try {
            interpreter.set("ctx", this);
            interpreter.set("codeEditText", codeEditText);
            interpreter.set("container", linearLayout);

        } catch (EvalError evalError) {
            evalError.printStackTrace();
        }

        OutputStream streamedOutStream = new OutputStream() {
            @Override
            public void write(int oneByte) throws IOException {
                streamedOutString += (char) oneByte;
                streamedOutTV.setText(MainActivity.this.streamedOutString);
            }
        };

        interpreter.setOut(new PrintStream(streamedOutStream));
        interpreter.setErr(new PrintStream(streamedOutStream));

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String showedCode = sharedPrefs.getString(CODE_KEY, getString(R.string.hello_world_code));
        codeEditText.setText(showedCode);

        codeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                PreferenceManager.getDefaultSharedPreferences(this).
                        edit()
                        .putString(CODE_KEY, codeEditText.getText().toString())
                        .commit();
                return true;
            case R.id.action_load:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                try {
                    final String[] fileNames = getAssets().list("scripts");
                    final ArrayAdapter<String> scriptsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNames);
                    builder.setAdapter(scriptsAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {

                                final InputStream inputStream = getAssets().open("scripts/" + fileNames[which]);

                                StringWriter writer = new StringWriter();
                                IOUtils.copy(inputStream, writer, "UTF-8");
                                String theString = writer.toString();

                                codeEditText.setText(theString);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
