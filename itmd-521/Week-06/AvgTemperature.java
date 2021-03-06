/*
** Developed By: Milad Sabouri
** A20389859
** HW-06
** Part 1
*/


import java.io.IOException;

import com.cloudera.sqoop.lib.RecordParser.ParseError;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.*;

public class AvgTemperature extends Configured implements Tool {

  public static class AvgTempMapper
      extends Mapper<LongWritable, Text, LongWritable, Record> {

    private Record avgTemp = null;

    public void map(LongWritable k, Text v, Context context) {
      Record record = new Record();
      try {
        record.parse(v); // Auto-generated: parse all fields from text.
      } catch (ParseError pe) {
        // Got a malformed record. Ignore it.
        return;
      }

      Integer temperature = record.get_temperature();
      if (null == temperature) {
        return;
      } else {
        //Filter unvalidated data
        if (temperature.intValue() != 9999) {
          avgTemp = record;
        }
      }
    }

    public void cleanup(Context context)
        throws IOException, InterruptedException {
      if (null != avgTemp) {
        context.write(new LongWritable(0), avgTemp);
      }
    }
  }

  public static class AvgTempReducer
      extends Reducer<LongWritable, Record, FloatWritable, NullWritable> {

    public void reduce(LongWritable k, Iterable<Record> vals, Context context)
        throws IOException, InterruptedException {
      
      //Sum all temperatures
      int sum = 0; 
      //Count the total number of records
      int count = 0;

      for (Record r : vals) {
        sum = sum + r.get_temperature().intValue();
        count++;
      }

      //Calculate Average of Temperatures
      Float avg = (float) sum / count;
      context.write(new FloatWritable(avg), NullWritable.get());
    }
  }

  public int run(String [] args) throws Exception {
    Job job = new Job(getConf());

    job.setJarByClass(AvgTemperature.class);

    job.setMapperClass(AvgTempMapper.class);
    job.setReducerClass(AvgTempReducer.class);

    FileInputFormat.addInputPath(job, new Path("records"));
    FileOutputFormat.setOutputPath(job, new Path("avgtemp"));

    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(Record.class);

    job.setOutputKeyClass(Record.class);
    job.setOutputValueClass(NullWritable.class);

    job.setNumReduceTasks(1);

    if (!job.waitForCompletion(true)) {
      return 1; // error.
    }

    return 0;
  }

  public static void main(String [] args) throws Exception {
    int ret = ToolRunner.run(new AvgTemperature(), args);
    System.exit(ret);
  }
}
