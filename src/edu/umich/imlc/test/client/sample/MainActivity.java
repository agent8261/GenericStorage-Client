package edu.umich.imlc.test.client.sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

import edu.umich.imlc.mydesk.test.common.GenericContract;
import edu.umich.imlc.mydesk.test.common.Utils;
import edu.umich.imlc.mydesk.test.common.GenericContract.MetaData;
import edu.umich.imlc.mydesk.test.common.GenericContract.MetaDataColumns;
import edu.umich.imlc.mydesk.test.common.exceptions.MyDeskException;
import edu.umich.imlc.test.api.GenericStorageApi;
import edu.umich.imlc.test.client.R;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends ListActivity implements
    LoaderCallbacks<Cursor>
{
  private static final int GENERIC_LOADER = 0;
  public static final String TAG = "GenericStorageClient";
  static final String SAMPLE_TYPE = "sample";
  GenericStorageApi api;
  Random rand = new Random();
  public static final String[] mFromColumns = { MetaDataColumns.NAME, MetaDataColumns.URI };
  public static final int[] mToFields = { android.R.id.text1, android.R.id.text2 };
  public static final String[] loaderProjection = { MetaDataColumns.ID, MetaDataColumns.NAME,
      MetaDataColumns.URI };
  SimpleCursorAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    Utils.printMethodName();
    mAdapter = new SimpleCursorAdapter(this,
        android.R.layout.two_line_list_item, null, mFromColumns, mToFields, 0);
    getLoaderManager().initLoader(GENERIC_LOADER, null, this);
    setListAdapter(mAdapter);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  public void createFile(MenuItem item)
  {
    Utils.printMethodName();
    Log.v(TAG, "go");
    String fileName = "blah" + Math.abs(rand.nextInt()) + ".sample";

    File blahFile = null;
    try
    {
      blahFile = createLocalFile(fileName,
          fileName + " create, " + 0);
    }
    catch( FileNotFoundException e )
    {
      e.printStackTrace();
      return;
    }
    catch( IOException e )
    {
      e.printStackTrace();
      return;
    }

    MetaData metaData = null;
    try
    {
      metaData = api().createNewFile(fileName, SAMPLE_TYPE, blahFile);
    }
    catch( MyDeskException e )
    {
      e.printStackTrace();
      return;
    }
    Log.v(TAG, "Create success, id: " + metaData.fileId());
    blahFile.delete();
  }

  public void doSync(MenuItem item)
  {
    Utils.printMethodName();
    api().requestSync();
  }

  public void login(MenuItem item)
  {
    Utils.printMethodName();
    api().startLoginActivity();
  }

  public void displayAccount(MenuItem item)
  {
    Utils.printMethodName();
    String accountName = api().getCurrentAccount();
    if( accountName.isEmpty() )
    {
      accountName = "No account";
    }
    AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
    aBuilder.setMessage(accountName).setTitle("Account Name").show();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args)
  {
    Utils.printMethodName();
    switch ( id )
    {
      case GENERIC_LOADER:
        CursorLoader loader = new CursorLoader(this);
        loader.setUri(GenericContract.URI_FILES);

        loader.setProjection(loaderProjection);
        return loader;
      default:
        return null;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
  {
    Utils.printMethodName();
    mAdapter.changeCursor(cursor);
  }


  public void openAndPrint(MetaData metaData)
  {
    Utils.printMethodName();
    try
    {
      InputStream is = api().loadFile(metaData.fileId());
      try
      {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));

        StringBuilder result = new StringBuilder();
        String line;
        String newLine = "";
        while( (line = r.readLine()) != null )
        {
          result.append(newLine).append(line);
          newLine = "\n";
        }
        AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
        aBuilder.setMessage(result.toString()).show();
      }
      finally
      {
        is.close();
      }
    }
    catch( FileNotFoundException e )
    {
      e.printStackTrace();
    }
    catch( IOException e )
    {
      e.printStackTrace();
    }
    catch( MyDeskException e )
    {
      e.printStackTrace();
    }
  }

  public void updateFile(MetaData fileMetaData)
  {
    Utils.printMethodName();
    File blahFile = null;
    try
    {
      blahFile = createLocalFile(fileMetaData.fileName(),
          fileMetaData.fileName() + " update, " + fileMetaData.sequenceNumber());
      api().saveFile(blahFile);
    }
    catch( IOException e )
    {
      e.printStackTrace();
      return;
    }
    catch( MyDeskException e )
    {
      e.printStackTrace();
    }
  }

  public File createLocalFile(String fileName, String content)
      throws IOException
  {
    Utils.printMethodName();
    File externalDir = getExternalCacheDir();
    File blahFile = new File(externalDir, "blah");
    if( blahFile.exists() )
    {
      blahFile.delete();
    }
    BufferedWriter bw = new BufferedWriter(new FileWriter(blahFile));
    try
    {
      bw.write("this is " + fileName);
      bw.newLine();
      bw.write(content);
      bw.newLine();
    }
    finally
    {
      bw.close();
    }
    return blahFile;
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0)
  {
    Utils.printMethodName();
    mAdapter.changeCursor(null);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id)
  {
    Utils.printMethodName();
    MetaData fileMetaData = api().getMetaData(id);
    openAndPrint(fileMetaData);
    updateFile(fileMetaData);
  }

  private GenericStorageApi api()
  {
    if( api == null )
    {
      api = new GenericStorageApi(this);
    }
    return api;
  }
}
