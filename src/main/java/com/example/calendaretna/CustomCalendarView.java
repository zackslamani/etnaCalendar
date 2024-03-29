package com.example.calendaretna;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;




public class CustomCalendarView extends LinearLayout {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    String mailEventCreator = user.getEmail();
    ImageButton NextButton, PreviousButton;
    TextView CurrentDate;
    GridView gridView;
    Button Logout;
    private static final int MAX_CALENDAR_DAYS = 42;
    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
    Context context;
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
    SimpleDateFormat yearFormate = new SimpleDateFormat("yyyy", Locale.ENGLISH);
    SimpleDateFormat eventDateFormate = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    MyGridAdapter myGridAdapter;
    AlertDialog alertDialog;
    List<Date> dates = new ArrayList<>();
    List<Event> eventsList = new ArrayList<>();
    int alarmYear, alarmMonth, alarmDay, alarmHour, alarmMinuit;

    DBOpenHelper dbOpenHelper;

    public CustomCalendarView(Context context) {
        super(context);
    }

    public CustomCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        IntializeLayout();
        SetUpCalendar();

        PreviousButton.setOnClickListener(view -> {
            calendar.add(Calendar.MONTH, -1);
            SetUpCalendar();
        });
        NextButton.setOnClickListener(view -> {
            calendar.add(Calendar.MONTH, 1);
            SetUpCalendar();
        });

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(true);
            View addView = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_newevent_layout, null);
            EditText EventName = addView.findViewById(R.id.eventname);
            TextView EventTime = addView.findViewById(R.id.eventtime);
            ImageButton SetTime = addView.findViewById(R.id.seteventtime);
            CheckBox alarmMe = addView.findViewById(R.id.alarmme);
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.setTime(dates.get(position));
            alarmYear = dateCalendar.get(Calendar.YEAR);
            alarmMonth = dateCalendar.get(Calendar.MONTH);
            alarmDay = dateCalendar.get(Calendar.DAY_OF_MONTH);

            Button AddEvent = addView.findViewById(R.id.addevent);
            SetTime.setOnClickListener(view12 -> {
                Calendar calendar = Calendar.getInstance();
                int hours = calendar.get(Calendar.HOUR_OF_DAY);
                int minuts = calendar.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(addView.getContext(), R.style.Theme_AppCompat_Dialog, (view121, hourOfDay, minute) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    c.set(Calendar.MINUTE, minute);
                    c.setTimeZone(TimeZone.getDefault());
                    SimpleDateFormat hformate = new SimpleDateFormat("K:mm a", Locale.ENGLISH);
                    String event_Time = hformate.format(c.getTime());
                    EventTime.setText(event_Time);
                    alarmHour = c.get(Calendar.HOUR_OF_DAY);
                    alarmMinuit = c.get(Calendar.MINUTE);
                },hours, minuts, false);
                timePickerDialog.show();
            });
            final String date = eventDateFormate.format(dates.get(position));
            final String month = monthFormat.format(dates.get(position));
            final String year = yearFormate.format(dates.get(position));


            AddEvent.setOnClickListener(view1 -> {

                if (alarmMe.isChecked()){
                    SaveEvent(EventName.getText().toString(),EventTime.getText().toString(),date, month, year, "on", mailEventCreator);
                    SetUpCalendar();
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(alarmYear, alarmMonth, alarmDay, alarmHour, alarmMinuit);
                    setAlarm(calendar, EventName.getText().toString(), EventTime.getText().toString(),getRequestCode(date
                            , EventName.getText().toString(), EventTime.getText().toString()));
                    alertDialog.dismiss();
                }else {
                    SaveEvent(EventName.getText().toString(),EventTime.getText().toString(),date, month, year, "off", mailEventCreator);
                    SetUpCalendar();
                    alertDialog.dismiss();
                }


            });

            builder.setView(addView);
            alertDialog = builder.create();
            alertDialog.show();
        });

        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            String date = eventDateFormate.format(dates.get(position));
            String mailEventCreator = user.getEmail();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(true);
            View showView = LayoutInflater.from(parent.getContext()).inflate(R.layout.show_events_layout, null);
            RecyclerView recyclerView = showView.findViewById(R.id.EventsRV);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(showView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setHasFixedSize(true);
            EventRecyclerAdapter eventRecyclerAdapter = new EventRecyclerAdapter(showView.getContext(),CollectAllEventByDate(date, mailEventCreator));
            recyclerView.setAdapter(eventRecyclerAdapter);
            eventRecyclerAdapter.notifyDataSetChanged();

            builder.setView(showView);
            alertDialog = builder.create();
            alertDialog.show();
            alertDialog.setOnCancelListener(dialogInterface -> SetUpCalendar());

            return true;
        });
        Logout.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Log.i("logout", "button pressed");
                mAuth.signOut();
                Log.i("logout", "auth pressed");
                context.startActivity(new Intent(context,LoginActivity.class));
                Log.i("logout", "activity pressed");
            }
        });
    }

    private int getRequestCode(String date, String event, String time){
        int code = 0;
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadIDEvents(date,event, time, database);
        while (cursor.moveToNext()){
            code = cursor.getInt(cursor.getColumnIndex(DBStructure.IDEVENT));
        }
        cursor.close();
        dbOpenHelper.close();

        return code;
    }

    private void setAlarm(Calendar calendar, String event, String time, int RequestCOde){
        Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        intent.putExtra("event", event);
        intent.putExtra("time", time);
        intent.putExtra("id", RequestCOde);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, RequestCOde, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private ArrayList<String> IDEventList(String date, String MailGuest){
        ArrayList<String> listIDEvent = new ArrayList<>();
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.getMailEventfromGuestTab(date, MailGuest, database);
        while (cursor.moveToNext()){
            String IDEvent = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            Log.i("arrayID", "IDevent : "+ IDEvent );
            listIDEvent.add(IDEvent);
        }
        return listIDEvent;
    }



    private ArrayList<Event> CollectAllEventByDate(String date, String maileventCreator){
        ArrayList<Event> listAllEvent = CollectEventByDate(date, maileventCreator);
        ArrayList<String> listIDEvent = IDEventList(date, maileventCreator);
        ArrayList<Event> listGuestEvent = new ArrayList<>();


        if (listIDEvent.isEmpty()){
            return listAllEvent;
        }else{
            for (int i = 0; i < listIDEvent.size();i++){
               Event event = getEventbyID(listIDEvent.get(i), date);
               listGuestEvent.add(event);
            }

            for (int i = 0; i<listGuestEvent.size(); i++){
                Event event = listGuestEvent.get(i);
                listAllEvent.add(event);

            }

        }
        return listAllEvent;
    }
    private ArrayList<Event> CollectEventByDate(String date, String maileventCreator){
        ArrayList<Event> arrayList = new ArrayList<>();
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadEvents(date, maileventCreator,database);
        while (cursor.getColumnCount() != 0 && cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            String eventName = cursor.getString(cursor.getColumnIndex(DBStructure.EVENTNAME));
            String time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME));
            String Date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR));
            String mailEventCreator = cursor.getString(cursor.getColumnIndex(DBStructure.MAIL_EVENT_CREATOR));
            Event event = new Event(id,eventName, time, Date, month, Year, mailEventCreator);
            arrayList.add(event);
        }
        cursor.close();
        dbOpenHelper.close();
        return arrayList;
    }


    private Event getEventbyID(String IDEvent, String date) {
        Event event = null;
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.getEventByID(IDEvent, date, database);
        while (cursor.getColumnCount() != 0 && cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            String eventName = cursor.getString(cursor.getColumnIndex(DBStructure.EVENTNAME));
            String time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME));
            String Date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR));
            String mailEventCreator = cursor.getString(cursor.getColumnIndex(DBStructure.MAIL_EVENT_CREATOR));
            event = new Event(id,eventName, time, Date, month, Year, mailEventCreator);
        }
        return event;
    }

    public CustomCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void SaveEvent(String eventName, String time, String date, String month, String year, String notify, String mailEventCreator){
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
        dbOpenHelper.SaveEvent(eventName, time, date, month, year, notify, mailEventCreator, database);
        dbOpenHelper.close();
        Toast.makeText(context, "Event Saved", Toast.LENGTH_SHORT).show();

    }

    private void IntializeLayout(){
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.calendar_layout, this);
        NextButton = view.findViewById(R.id.nextBtn);
        PreviousButton = view.findViewById(R.id.previousBtn);
        CurrentDate = view.findViewById(R.id.current_date);
        Logout = view.findViewById(R.id.logout);
        gridView = view.findViewById(R.id.gridview);

    }

    private void SetUpCalendar(){
        String currentDate = dateFormat.format(calendar.getTime());
        CurrentDate.setText(currentDate);
        dates.clear();
        Calendar monthCalendar= (Calendar) calendar.clone();
        monthCalendar.set(Calendar.DAY_OF_MONTH,1);
        int FirstDayofMonth = monthCalendar.get(Calendar.DAY_OF_WEEK)-1;
        monthCalendar.add(Calendar.DAY_OF_MONTH, -FirstDayofMonth);
        CollectAllEventsPerMonth(monthFormat.format(calendar.getTime()), yearFormate.format(calendar.getTime()), mailEventCreator);

        while (dates.size() < MAX_CALENDAR_DAYS){
            dates.add(monthCalendar.getTime());
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        myGridAdapter = new MyGridAdapter(context, dates, calendar, eventsList);
        gridView.setAdapter(myGridAdapter);
    }

    private void CollectEventsPerMonth(String Month, String year, String maileventCreator){
        eventsList.clear();
        dbOpenHelper=new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadEventsperMonth(Month, year, maileventCreator, database);
        while (cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            String eventName = cursor.getString(cursor.getColumnIndex(DBStructure.EVENTNAME));
            String time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME));
            String date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR));
            String mailEventCreator = cursor.getString(cursor.getColumnIndex(DBStructure.MAIL_EVENT_CREATOR));
            Event event = new Event(id,eventName, time, date, month, Year, mailEventCreator);
            eventsList.add(event);
        }
        cursor.close();
        dbOpenHelper.close();
    }

    private void CollectEventsGuestPerMonth(String IDevent, String Month, String year){
        dbOpenHelper=new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadEventsperMonthID(IDevent, Month, year, database);
        while (cursor.moveToNext()){
            String id = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            String eventName = cursor.getString(cursor.getColumnIndex(DBStructure.EVENTNAME));
            String time = cursor.getString(cursor.getColumnIndex(DBStructure.TIME));
            String date = cursor.getString(cursor.getColumnIndex(DBStructure.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBStructure.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBStructure.YEAR));
            String mailEventCreator = cursor.getString(cursor.getColumnIndex(DBStructure.MAIL_EVENT_CREATOR));
            Event event = new Event(id,eventName, time, date, month, Year, mailEventCreator);
            eventsList.add(event);
        }
        cursor.close();
        dbOpenHelper.close();
    }

    private void CollectAllEventsPerMonth(String Month, String year, String maileventCreator){

        CollectEventsPerMonth(Month, year, maileventCreator);
        ArrayList<String> listIDEvent = IDEventGuestList(maileventCreator);
       for (int i = 0; i <listIDEvent.size(); i++){
           CollectEventsGuestPerMonth(listIDEvent.get(i), Month, year);
       }
    }

    private ArrayList<String> IDEventGuestList(String MailGuest){
        ArrayList<String> listIDEvent = new ArrayList<>();
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.getIDEventGuest(MailGuest, database);
        while (cursor.moveToNext()){
            String IDEvent = cursor.getString(cursor.getColumnIndex(DBStructure.IDEVENT));
            listIDEvent.add(IDEvent);
        }
        return listIDEvent;
    }

}
