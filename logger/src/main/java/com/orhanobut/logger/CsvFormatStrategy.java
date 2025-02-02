package com.orhanobut.logger;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.orhanobut.logger.Utils.checkNotNull;

/**
 * CSV formatted file logging for Android.
 * Writes to CSV the following data:
 * epoch timestamp, ISO8601 timestamp (human-readable), log level, tag, log message.
 */
public class CsvFormatStrategy implements FormatStrategy {

  private static final String NEW_LINE = System.getProperty("line.separator");
  private static final String NEW_LINE_REPLACEMENT = " <br> ";
  private static final String SEPARATOR = ",";

  @NonNull private final Date date;
  @NonNull private final SimpleDateFormat dateFormat;
  @NonNull private final LogStrategy logStrategy;
  @NonNull private final String newLineReplacement;
  @Nullable private final String tag;

  private CsvFormatStrategy(@NonNull Builder builder) {
    checkNotNull(builder);

    date = builder.date;
    dateFormat = builder.dateFormat;
    logStrategy = builder.logStrategy;
    tag = builder.tag;
    newLineReplacement = builder.newLineReplacement;
  }

  @NonNull public static Builder newBuilder() {
    return new Builder();
  }

  @Override public void log(int priority, @Nullable String onceOnlyTag, @NonNull String message) {
    checkNotNull(message);

    String tag = formatTag(onceOnlyTag);

    date.setTime(System.currentTimeMillis());

    StringBuilder builder = new StringBuilder();

    // machine-readable date/time
    builder.append(date.getTime());

    // human-readable date/time
    builder.append(SEPARATOR);
    builder.append(dateFormat.format(date));

    // level
    builder.append(SEPARATOR);
    builder.append(Utils.logLevel(priority));

    // tag
    builder.append(SEPARATOR);
    builder.append(tag);

    // message
    if (message.contains(NEW_LINE)) {
      // a new line would break the CSV format, so we replace it here
      message = message.replaceAll(NEW_LINE, newLineReplacement);
    }
    builder.append(SEPARATOR);
    builder.append(message);

    // new line
    builder.append(NEW_LINE);

    logStrategy.log(priority, tag, builder.toString());
  }

  @Nullable private String formatTag(@Nullable String tag) {
    if (!Utils.isEmpty(tag) && !Utils.equals(this.tag, tag)) {
      return this.tag + "-" + tag;
    }
    return this.tag;
  }

  public static final class Builder {
    private static final int MAX_BYTES = 500 * 1024; // 500K averages to a 4000 lines per file

    Date date;
    SimpleDateFormat dateFormat;
    LogStrategy logStrategy;
    String tag = "PRETTY_LOGGER";
    String folder = "";
    String fileName = "";
    int maxBytes = MAX_BYTES;
    String newLineReplacement = " <br> ";

    private Builder() {
    }

    @NonNull public Builder date(@Nullable Date val) {
      date = val;
      return this;
    }

    @NonNull public Builder dateFormat(@Nullable SimpleDateFormat val) {
      dateFormat = val;
      return this;
    }

    @NonNull public Builder logStrategy(@Nullable LogStrategy val) {
      logStrategy = val;
      return this;
    }

    @NonNull public Builder tag(@Nullable String tag) {
      this.tag = tag;
      return this;
    }

    @NonNull public Builder folder(@Nullable String folder) {
      this.folder = folder;
      return this;
    }

    @NonNull public Builder flieName(@Nullable String fileName) {
      this.fileName = fileName;
      return this;
    }

    @NonNull public Builder maxBtyes(@Nullable int maxBytes) {
      this.maxBytes = maxBytes;
      return this;
    }

    @NonNull public Builder newLineReplacement(@Nullable String newLineReplacement) {
      this.newLineReplacement = newLineReplacement;
      return this;
    }

    @NonNull public CsvFormatStrategy build() {
      if (date == null) {
        date = new Date();
      }
      if (dateFormat == null) {
        dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.UK);
      }
      if (logStrategy == null) {
        if (TextUtils.isEmpty(folder)) {
          folder = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar + "logger";
        }
        if (TextUtils.isEmpty(fileName)) {
          fileName = "logs";
        }
        //追加日期
        fileName = fileName + "_" + getStringByFormat("yyyyMMdd");

        HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
        ht.start();
        Handler handler = new DiskLogStrategy.WriteHandler(ht.getLooper(), folder, fileName, maxBytes);
        logStrategy = new DiskLogStrategy(handler);
      }
      return new CsvFormatStrategy(this);
    }
  }

  public static String getStringByFormat(String format) {
    String thisDateTime = null;
    try {
      SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(format);
      thisDateTime = mSimpleDateFormat.format(System.currentTimeMillis());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return thisDateTime;
  }
}
