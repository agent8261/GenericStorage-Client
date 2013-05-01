package edu.umich.imlc.test.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import edu.umich.imlc.mydesk.test.common.GenericContract;
import edu.umich.imlc.mydesk.test.common.GenericContract.MetaDataColumns;
import edu.umich.imlc.mydesk.test.common.Utils;
import edu.umich.imlc.mydesk.test.common.GenericContract.Exceptions;
import edu.umich.imlc.mydesk.test.common.GenericContract.MetaData;
import edu.umich.imlc.mydesk.test.common.GenericContract.MetaDataProjections;
import edu.umich.imlc.mydesk.test.common.exceptions.MyDeskException;
import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class GenericStorageApi
{
  public static final String TAG = "GenericStorageApi";
  private Context mContext;
  private MetaData currentFile;

  // ---------------------------------------------------------------------------
  // ---------------------------------------------------------------------------

  public GenericStorageApi(Context c_)
  {
    Utils.printMethodName();
    mContext = c_;
  }

  // ---------------------------------------------------------------------------

  public void startLoginActivity()
  {
    Intent i = new Intent();
    i.setComponent(GenericContract.COMPONENT_LOGIN_ACTIVITY);
    if( !(mContext instanceof Activity) )
    {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    mContext.startActivity(i);
  }

  // ---------------------------------------------------------------------------

  public void requestSync()
  {
    Account account = new Account(getCurrentAccount(), "com.google");
    ContentResolver.requestSync(account, GenericContract.AUTHORITY,
        new Bundle());
  }

  // ---------------------------------------------------------------------------

  public String getCurrentAccount()
  {
    Cursor c = mContext.getContentResolver().query(
        GenericContract.URI_CURRENT_ACCOUNT, null, null, null, null);
    String res = "";
    if( c.moveToFirst() )
    {
      res = c.getString(0);
      c.close();
    }
    return res;
  }// getCurrentAccount

  // ---------------------------------------------------------------------------

  public InputStream loadFile(String id) throws FileNotFoundException,
      MyDeskException
  {
    Utils.printMethodName();
    try
    {
      if( id.isEmpty() )
      {
        throw new IllegalArgumentException();
      }

      Uri uri = Uri.withAppendedPath(GenericContract.URI_FILES, id);
      currentFile = getMetaData(id);
      return mContext.getContentResolver().openInputStream(uri);
    }
    catch( IllegalStateException e )
    {
      throwMyDeskExceptionIfAvailable(e);
      return null;
    }
  }// loadFile

  // ---------------------------------------------------------------------------

  public MetaData createNewFile(String name, String type, File newFile)
      throws MyDeskException
  {
    Utils.printMethodName();
    try
    {
      if( name.isEmpty() )
      {
        throw new IllegalArgumentException();
      }
      if( type.isEmpty() )
      {
        throw new IllegalArgumentException();
      }

      ContentValues values = new ContentValues();
      values.put(MetaDataColumns.NAME, name);
      values.put(MetaDataColumns.TYPE, type);
      values.put(GenericContract.KEY_UPDATE_BACKEND, false);
      values
          .put(GenericContract.KEY_NEW_FILE, Uri.fromFile(newFile).toString());
      String newId = mContext.getContentResolver()
          .insert(GenericContract.URI_FILES, values).getLastPathSegment();
      currentFile = getMetaData(newId);
      return currentFile;
    }
    catch( IllegalStateException e )
    {
      throwMyDeskExceptionIfAvailable(e);
      return null;
    }
  } // createNewFile

  // ---------------------------------------------------------------------------

  /**
   * saves file into the last opened/created/saved file id, overwrites the
   * previously saved file. A copy of the file will be created in the client's
   * space, with the original file left untouched. It is safe to delete the file
   * after this method returns
   * 
   * @param file
   * @throws MyDeskException
   */
  public void saveFile(File file) throws MyDeskException
  {
    Utils.printMethodName();
    if( !file.exists() )
    {
      throw new IllegalArgumentException();
    }

    ContentValues values = new ContentValues();
    Uri fileUri = Uri.fromFile(file);
    values.put(GenericContract.KEY_NEW_FILE, fileUri.toString());
    values.put(GenericContract.KEY_UPDATE_OLD_SEQUENCE,
        currentFile.sequenceNumber());
    values.put(GenericContract.KEY_UPDATE_BACKEND, false);
    try
    {
      mContext.getContentResolver()
          .update(
              Uri.withAppendedPath(GenericContract.URI_FILES,
                  currentFile.fileId()), values, null, null);

      // refresh our current file metadata
      currentFile = getMetaData(currentFile.fileId());
    }
    catch( IllegalStateException e )
    {
      throwMyDeskExceptionIfAvailable(e);
    }
  } // saveFile

  // ---------------------------------------------------------------------------

  public MetaData getMetaData(String fileId) throws MyDeskException
  {
    Utils.printMethodName();
    if( fileId.isEmpty() )
    {
      throw new IllegalArgumentException();
    }

    String[] whereArgs = { fileId };
    Cursor c = mContext.getContentResolver().query(
        Uri.withAppendedPath(GenericContract.URI_FILES, fileId),
        MetaDataProjections.METADATA, MetaDataColumns.FILE_ID + "=?",
        whereArgs, null);
    try
    {
      if( !c.moveToFirst() )
        return null;
      return new MetaData(c);
    }
    finally
    {
      c.close();
    }// getMetaData
  }

  // ---------------------------------------------------------------------------

  public MetaData getMetaData(long localId)
  {
    Utils.printMethodName();
    String[] selectionIn = { String.valueOf(localId) };
    Cursor c = mContext.getContentResolver().query(GenericContract.URI_FILES,
        MetaDataProjections.METADATA, MetaDataColumns.ID + "=?", selectionIn,
        null);
    try
    {
      if( !c.moveToFirst() )
        return null;
      return new MetaData(c);
    }
    finally
    {
      c.close();
    }
  }// getMetaData

  // ---------------------------------------------------------------------------

  /**
   * Checks if the IllegalStateException is caused by a MyDeskException and
   * throws the MyDeskException, otherwise throws the IllegalStateException
   * 
   * @param ise
   * @throws MyDeskException
   */
  private void throwMyDeskExceptionIfAvailable(IllegalStateException ise)
      throws MyDeskException
  {
    Utils.printMethodName();
    MyDeskException e = null;
    try
    {
      e = Exceptions.valueOf(ise.getMessage()).createException();
      throw e;
    }
    catch( IllegalArgumentException ex )
    {
      throw ise;
    }
  }// throwMyDeskExceptionIfAvailable

}