package com.cookandroid.weplog

import android.content.ContentValues.TAG
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dinuscxj.progressbar.CircleProgressBar
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MyValue_steptype(private val xValsDateLabel: ArrayList<String>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toString()
    }
    override fun getAxisLabel(value: Float, axis: AxisBase): String {
        if (value.toInt() >= 0 && value.toInt() <= xValsDateLabel.size - 1) {
            return xValsDateLabel[value.toInt()]
        } else {
            return ("").toString()
        }
    }
}


class MyValueFormatter2(private val xValsDateLabel: ArrayList<String>) : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return value.toString()
    }

    override fun getAxisLabel(value: Float, axis: AxisBase): String {
        if (value.toInt() >= 0 && value.toInt() <= xValsDateLabel.size - 1) {
            return xValsDateLabel[value.toInt()]
        } else {
            return ("").toString()
        }
    }
}
class MyValueFormatter3(private val xValsDateLabel: ArrayList<String>) : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return value.toString()
    }

    override fun getAxisLabel(value: Float, axis: AxisBase): String {
        if (value.toInt() >= 1 && value.toInt() <= xValsDateLabel.size) {
            return xValsDateLabel[value.toInt()-1]
        } else {
            return ("").toString()
        }
    }
}


class RecordDetailActivity : AppCompatActivity() {
    //private var myAdapter : ArrayAdapter<Any> ?= null
    private lateinit var database: DatabaseReference
    private var plog : TextView ?= null
    private var distance : TextView?= null
    private var step_data : TextView?= null
    private var totalkcal : TextView ?= null
    var items : ArrayList<Any> ?= null
    var item = arrayOf("1", "2")
    private var mCircleProgressBar : CircleProgressBar?= null // ?????? ????????? (????????? ?????? ???)
    var mhorizontalBar : HorizontalBarChart?= null // ?????? ??? ????????? (step type)
    var mlineChart : LineChart?= null // ????????? step

    var detail_day : String ?= null
    var detail_month : String ?= null
    var detail_year : String ?= null
    var step : Int ?= null
    var plogs : Int ?= null
    var step_list = ArrayList<HashMap<String, String>>()

    override fun onDestroy() {
        super.onDestroy()
        //supportActionBar!!.show()

    }

    override fun onResume() {
        super.onResume()
        //supportActionBar!!.hide()
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        var actionBar = supportActionBar
        actionBar!!.setTitle("?????? ?????? ???")
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        //supportActionBar!!.hide()
        database = Firebase.database.reference
        mCircleProgressBar = findViewById(R.id.rec_graph) // ?????? ????????? (????????? ?????? ???)
        mhorizontalBar = findViewById(R.id.rec_graph_detail)
        mlineChart = findViewById(R.id.rec_graph_detail2)
        step_data = findViewById(R.id.step_data)
        distance = findViewById(R.id.distance)
        totalkcal = findViewById(R.id.totalkcal)
        plog = findViewById(R.id.detail_plog)

        if(intent.hasExtra("day") && intent.hasExtra("year") && intent.hasExtra("month")){
            detail_day = intent.getStringExtra("day").toString()
            detail_month = intent.getStringExtra("month").toString()
            detail_year = intent.getStringExtra("year").toString()
            step = intent.getIntExtra("step", 0).toInt()
            plogs = intent.getIntExtra("plog", 0)
            step_list = intent.getSerializableExtra("step_list") as ArrayList<HashMap<String, String>>
            println("tt :" + step_list)
            update(detail_day.toString(), detail_month.toString(), detail_year.toString(), step!!)
        } else {
            Toast.makeText(this, "????????? ????????? ??????", Toast.LENGTH_SHORT).show()
        }

    }

    fun update(day : String, month : String, year : String, step : Int){
        var stepTime = ArrayList<String>()
        var stepAcc = ArrayList<Float>()

        // CircleGraph
        mCircleProgressBar!!.max = 500
        mCircleProgressBar!!.progress = step
        step_data!!.text = step.toString()
        plog!!.text = plogs.toString()
        mCircleProgressBar!!.setProgressFormatter(CircleProgressBar.ProgressFormatter { progress, max ->
            val pattern = "%d Steps"
            String.format(pattern, progress)
        })

        var w = 0
        var j = 0
        var r = 0
        for ( s in step_list){
            if ( s.get("type")!!.toInt() == 0 ){
                w  = w + 1
                try {
                    stepTime.add(SimpleDateFormat("HH:mm:ss").format(s.get("time").toString().toLong()))
                    stepAcc.add(s.get("peak").toString().toFloat())
                } catch ( e : NumberFormatException ) {

                }
            } else if (s.get("type")!!.toInt() == 1 ) {
                j = j + 1
                try {
                    stepTime.add(SimpleDateFormat("HH:mm:ss").format(s.get("time").toString().toLong()))
                    stepAcc.add(s.get("peak").toString().toFloat())
                } catch ( e : NumberFormatException ) {

                }
            } else {
                r = r + 1
                try {
                    stepTime.add(SimpleDateFormat("HH:mm:ss").format(s.get("time").toString().toLong()))
                    stepAcc.add(s.get("peak").toString().toFloat())
                } catch ( e : NumberFormatException ) {

                }
            }
        }
        var totalCaloriesBurned : Float = w*0.05f + j*0.1f + r*0.2f
        totalkcal!!.text = String.format("%.2f", totalCaloriesBurned) + " kcal"

        // horizonchart
        val bar_ylabels = ArrayList<String>()
        bar_ylabels.add(w.toString())
        bar_ylabels.add(j.toString())
        bar_ylabels.add(r.toString())
        var type = ArrayList<String>()
        type.add("??????")
        type.add("??????")
        type.add("??????")
        mhorizontalBar!!.setDrawBarShadow(false)
        mhorizontalBar!!.setDrawValueAboveBar(true)
        mhorizontalBar!!.description.isEnabled = false
        mhorizontalBar!!.setPinchZoom(true)
        mhorizontalBar!!.setDrawGridBackground(false)
        var x1 = mhorizontalBar!!.xAxis
        x1.position = XAxis.XAxisPosition.BOTTOM
        x1.setDrawAxisLine(true)
        x1.setDrawGridLines(false)
        x1.setValueFormatter(MyValue_steptype(type))
        x1.granularity = 1f
        var y1 = mhorizontalBar!!.axisLeft
        y1.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        y1.setDrawGridLines(false)
        y1.setEnabled(false)
        y1.setAxisMinimum(0f)
        var yr = mhorizontalBar!!.axisRight
        yr.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        yr.setDrawGridLines(false)
        yr.setAxisMinimum(0f)
        yr.isEnabled = false
        var yentries1 = ArrayList<BarEntry>()
        yentries1.add(BarEntry(0f, w.toFloat()))
        var yentries2 = ArrayList<BarEntry>()
        yentries2.add(BarEntry(1f, j.toFloat()))
        var yentries3 = ArrayList<BarEntry>()
        yentries3.add(BarEntry(2f, r.toFloat()))
        var set1 = BarDataSet(yentries1, "??????")
        var set2 = BarDataSet(yentries2, "??????")
        var set3 = BarDataSet(yentries3, "??????")
        set1.setColor(Color.parseColor("#393BBE"))
        set2.setColor(Color.parseColor("#FA8072"))
        set3.setColor(Color.parseColor("#FFB700"))
        var datasets = ArrayList<IBarDataSet>()
        datasets.add(set1)
        datasets.add(set2)
        datasets.add(set3)
        var data = BarData(datasets)
        data.setValueTextSize(6f)
        data.setValueFormatter(MyValueFormatter2(bar_ylabels))
        data.barWidth = .6f
        //mhorizontalBar!!.axisRight.valueFormatter = (MyValueFormatter3(bar_ylabels))
        mhorizontalBar!!.data = data
        mhorizontalBar!!.notifyDataSetChanged()
        mhorizontalBar!!.invalidate()

        //line chart
        mlineChart!!.legend.isEnabled = false
        mlineChart!!.description.isEnabled = false
        mlineChart!!.axisLeft.isEnabled = false
        mlineChart!!.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        mlineChart!!.xAxis.granularity = 1f
        mlineChart!!.xAxis.isGranularityEnabled = true
        mlineChart!!.xAxis.setDrawGridLines(false)
        mlineChart!!.xAxis.position = XAxis.XAxisPosition.BOTTOM
        val ll1 = LimitLine(30f, "RUNNING PEAK")
        ll1.lineWidth = 1f
        ll1.enableDashedLine(10f, 10f, 0f)
        ll1.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        ll1.textSize = 5f
        val ll2 = LimitLine(25f, "JOGGING PEAK")
        ll2.lineWidth = 1f
        ll2.enableDashedLine(10f, 10f, 0f)
        ll2.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        ll2.textSize = 5f
        val ll3 = LimitLine(15f, "WALKING PEAK")
        ll3.lineWidth = 1f
        ll3.enableDashedLine(10f, 10f, 0f)
        ll3.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        ll3.textSize = 5f
        val leftAxis = mlineChart!!.axisLeft
        leftAxis.removeAllLimitLines()
        mlineChart!!.axisRight.removeAllLimitLines()
        leftAxis.addLimitLine(ll1)
        leftAxis.addLimitLine(ll2)
        leftAxis.addLimitLine(ll3)
        //leftAxis.axisMaximum = 50f
        leftAxis.axisMinimum = 0f
        val timevalues: ArrayList<Entry> = ArrayList()
        val line2_xlabels = ArrayList<String>()
        for (i in 0..stepTime.size-1){
            var t = stepTime[i]
            var t2 = stepAcc[i]
            println("time : " + t + " , " + t2)
            timevalues.add(Entry(i.toFloat(), t2))
            line2_xlabels.add(t)
        }
        var lineset2 = LineDataSet(timevalues, "Steps Time")
        lineset2.fillAlpha = 50
        var line_dataset2 = ArrayList<ILineDataSet>()
        line_dataset2.add(lineset2)
        var line_data2 = LineData(line_dataset2)
        lineset2.setColor(Color.parseColor("#1D4028"))
        lineset2.setCircleColor(Color.parseColor("#1D4028"))
        lineset2.setDrawFilled(true)
        lineset2.setDrawCircles(false)
        lineset2.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineset2.cubicIntensity = 0.2f
        lineset2.fillColor = Color.parseColor("#1D4028")
        mlineChart!!.data = line_data2
        mlineChart!!.xAxis.valueFormatter = (MyValueFormatter2(line2_xlabels))
        mlineChart!!.notifyDataSetChanged()
        mlineChart!!.invalidate()

        //Total Distance
        var date = year + "/" + month + "/" + day
        var total_d = 0f
        database.child("user").child(Firebase.auth.currentUser!!.uid).child("Pedometer").child("date").child(date).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (shot in snapshot.children) {
                    println("tt :" + shot.child("record").child("distance").value)
                    total_d = total_d + shot.child("record").child("distance").value.toString().toFloat()
                }
                distance!!.text = total_d.toString() + "KM"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", error.toException())
            }

        })
    }

}

