package com.rndapp.sendloc;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContactsAutoCompleteCursorAdapter extends CursorAdapter implements Filterable {

    private TextView mName, mNumber;

    public ContactsAutoCompleteCursorAdapter(Context context, Cursor c) {
        super(context, c);
        mContent = context.getContentResolver();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LinearLayout ret = new LinearLayout(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        mName = (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        mNumber = (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        ret.setOrientation(LinearLayout.VERTICAL);

        LinearLayout horizontal = new LinearLayout(context);
        horizontal.setOrientation(LinearLayout.HORIZONTAL);
        
        // you can even add images to each entry of your autocomplete fields
        // this example does it programmatically using JAVA, but the XML analog is very similar
        //ImageView icon = new ImageView(context);

        int nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        String name = cursor.getString(nameIdx);
        String number = cursor.getString(numberIdx);

        mName.setText(name);
        mNumber.setText(number);

        // setting the type specifics using JAVA
        mNumber.setTextSize(16);
        mNumber.setTextColor(Color.GRAY);

        // an example of how you can arrange your layouts programmatically
        // place the number and icon next to each other
        horizontal.addView(mNumber, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        ret.addView(mName, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ret.addView(horizontal, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        return ret;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        String name = cursor.getString(nameIdx);
        String number = cursor.getString(numberIdx);

        // notice views have already been inflated and layout has already been set so all you need to do is set the data
        ((TextView) ((LinearLayout) view).getChildAt(0)).setText(name);
        LinearLayout horizontal = (LinearLayout) ((LinearLayout) view).getChildAt(1);
        ((TextView) horizontal.getChildAt(0)).setText(number);
    }

    @Override
    public String convertToString(Cursor cursor) {
        // this method dictates what is shown when the user clicks each entry in your autocomplete list
        // in my case i want the number data to be shown
        int numCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
        int nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        String number = cursor.getString(numCol);
        String name = cursor.getString(nameCol);
        return name + " <" + number + "> ";
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        // this is how you query for suggestions
        // notice it is just a StringBuilder building the WHERE clause of a cursor which is the used to query for results
        if (getFilterQueryProvider() != null) { return getFilterQueryProvider().runQuery(constraint); }

        StringBuilder buffer = null;
        String[] args = null;
        if (constraint != null) {
            buffer = new StringBuilder();
            buffer.append(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " IS NOT NULL AND " + ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL AND ");
            buffer.append("UPPER(");
            buffer.append(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            buffer.append(") GLOB ?");
            args = new String[] { constraint.toString().toUpperCase() + "*" };
        }

        Cursor l = mContent.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone._ID}, buffer == null ? null : buffer.toString(), args, null);
//        boolean b = l.moveToFirst();
//        if (b){
//        	String name = l.getString(l.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
//        	String nume = l.getString(l.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//        	String togeth = name+nume;
//        	mName.setText(togeth);
//        }
        return l;
    }

    private ContentResolver mContent;

}

