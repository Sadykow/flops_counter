package com.example.perf_tflite;

import androidx.appcompat.app.AppCompatActivity;

//import org.apache.poi.hssf.usermodel.HSSFCell;
//import org.apache.poi.hssf.usermodel.HSSFRow;
//import org.apache.poi.hssf.usermodel.HSSFSheet;
//import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.poifs.filesystem.POIFSFileSystem;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
import org.tensorflow.lite.Interpreter;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    String TAG ="main";
    Button button;
    TextView outputText;
    TextView infoBox;
    Interpreter tflite;
    long startTimer;
    Spinner comboBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        outputText = (TextView) findViewById(R.id.outputText);
        infoBox = (TextView) findViewById(R.id.infoBox);
        comboBox = (Spinner) findViewById(R.id.comboBox);
        InputStream dataInput;
        float[][][] Data = new float[1][500][3];

        for(int i=0; i < 500; i++) {
            for(int j=0; j<3; j++) {
                Data[0][i][j] = 5;
            }
        }
        outputText.setText("Failed to read: "+Data[0][499][2]);

        // Click on button do verification:
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float prediction = doInference(Data);
                outputText.setText(Float.toString(prediction));
            }
        });

        // Click on ComboBox to get number to load number
        comboBox.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // An item was selected. You can retrieve the selected item using
                String ID = parent.getItemAtPosition(position).toString();
                String model_name = "Model-№"+ID+"-FUDS.tflite";
                // Creating interpreter
                try {
                    tflite = new Interpreter(loadModelFile(model_name));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                infoBox.setText(("Model load name: "+ model_name));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    // Model performs prediction by setting the inputs and outputs
    private float doInference(float[][][] inputVal) {
        // Input shape for me is [1,500,3]
        //float [] inputVal = new float[1];
        //inputVal[0] = Float.valueOf(inputString);

        // Output shape is [1]
        int N = 20;
        float[][] outputVal = new float[1][1];
        float[] avgVal = new float[N];
        String outputText = "";
        float sumVal = 0;
        float maxTime = 0;
        float minTime = 10000;
        float PMTime = 0;
        // Run inference passing the input shape and gettung the output shape
        try{
            // Usually 7632
            for(int i = 0; i<N; i++) {
                startTimer = System.currentTimeMillis();
                tflite.run(inputVal, outputVal);    // Single Pixel: 329ms
                                                    // Single Flare_4: 3926ms
                                                    // Single HTC: 1320ms
                avgVal[i] = System.currentTimeMillis() - startTimer;
                if(i % 2 == 0) outputText += ("\nTime: "+avgVal[i]+"ms!");
                sumVal += avgVal[i];
                if(maxTime < avgVal[i])  maxTime = avgVal[i];
                if(minTime > avgVal[i]) minTime = avgVal[i];
            }
            //int seconds = (int) (milis/1000);
            outputText += ("\nAverage Time: " + sumVal/N +"ms!");
            outputText += ("\nMax Time: " + maxTime + "ms" + " Min Time: " + minTime + "ms");
            PMTime = ((maxTime - sumVal/N) + (sumVal/N - minTime))/2;
            outputText += ("\nPM Time: " + PMTime + "ms");
            infoBox.setText(outputText);
            //infoBox.setText(("Time: "+avgVal[0]+"ms "+avgVal[1]+"ms "+avgVal[2]+"ms "+avgVal[3]+"ms "+avgVal[4]+"ms!"));
            // SIngle run: 329ms on a Pixel 2
        } catch (Exception e) {
            infoBox.setText(e.toString());
        }

        // Inferred value is at [0][0] and return
        return outputVal[0][0];
    }
    /**
     * Memory-Map the model file in Assets
     */
    private MappedByteBuffer loadModelFile(String model_name) throws IOException {
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(model_name);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

//    private void ReadExcelFile() {
//        // Reading excel file
//        try {
//            // Getting a Sheet
//            //dataInput = this.getAssets().open("A123_Matt_Val/A1-008-DST-US06-FUDS-30-20120820.xlsx");
//            dataInput = this.getAssets().open("test.xls");
//            POIFSFileSystem fileSystem = new POIFSFileSystem(dataInput);
//            HSSFWorkbook workBook = new HSSFWorkbook(fileSystem);
//            HSSFSheet mySheet = workBook.getSheetAt(1);
//
//            // Making iterators
//            Iterator<Row> rowIter = mySheet.rowIterator();
//            int rowN = 0;
//            while(rowIter.hasNext()) {
//                // Log
//                Log.e(TAG, "row N" + rowN);
//                HSSFRow myRow = (HSSFRow) rowIter.next();
//                if(rowN != 0) {
//                    Iterator<Cell> cellIter = myRow.cellIterator();
//                    int colN = 0;
//                    float voltage=0, current=0, temperature=0;
//                    while (cellIter.hasNext()) {
//                        HSSFCell myCell = (HSSFCell) cellIter.next();
////                        if(colN == 0) {
////                            voltage = Float.parseFloat(myCell.toString());
////                        } else if (colN == 1) {
////                            current = Double.parseDouble(myCell.toString());
////                        } else if (colN == 2) {
////                            temperature = Double.parseDouble(myCell.toString());
////                        }
//                        colN++;
//                        // Log
//                        Log.e(TAG, " Index :" + myCell.getColumnIndex() + " -- " + myCell.toString());
//                        Data[0][rowN][colN] = Float.parseFloat(myCell.toString());
//                    }
//                    //textView.append( voltage + " -- "+ current+ "  -- "+ temperature+"\n");
//                }
//                rowN++;
//            }
//            outputText.setText(""+Data[0][rowN][2]);
//        } catch (Exception e) {
//            Log.e(TAG, "error "+ e.toString());
//        }
//    }
}