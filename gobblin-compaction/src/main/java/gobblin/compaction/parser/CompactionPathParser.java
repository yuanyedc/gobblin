package gobblin.compaction.parser;

import gobblin.dataset.FileSystemDataset;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Preconditions;

import lombok.Getter;
import lombok.Setter;

import gobblin.compaction.mapreduce.MRCompactor;
import gobblin.configuration.State;


/**
 * A parser which converts {@link FileSystemDataset} to {@link CompactionParserResult}
 */
@AllArgsConstructor
public class CompactionPathParser {
  State state;

  /**
   * A parsed result returned by {@link CompactionPathParser#parse(FileSystemDataset)}
   */
  public static class CompactionParserResult {
    @Getter @Setter
    private  String srcBaseDir;
    @Getter @Setter
    private  String dstBaseDir;
    @Getter @Setter
    private  String srcSubDir;
    @Getter @Setter
    private  String dstSubDir;

    @Getter
    private DateTime time;
    @Getter
    private String timeString;
    @Getter
    private String datasetName;
  }

  /**
   * Parse a {@link FileSystemDataset} to some detailed parts like source base directory,
   * source sub directory, destination based directory, destination sub directory, and time
   * information.
   */
  public CompactionParserResult parse (FileSystemDataset dataset) {

    CompactionParserResult result = new CompactionParserResult();
    result.srcBaseDir = getSrcBaseDir (state);
    result.srcSubDir  = getSrcSubDir  (state);
    result.dstBaseDir = getDstBaseDir (state);
    result.dstSubDir  = getDstSubDir  (state);

    parseTimeAndDatasetName(dataset, result);

    return result;
  }

  private void parseTimeAndDatasetName (FileSystemDataset dataset, CompactionParserResult rst) {
    String commonBase = rst.getSrcBaseDir();
    String fullPath = dataset.datasetURN();
    int startPos = fullPath.indexOf(commonBase) + commonBase.length();
    String relative = StringUtils.removeStart(fullPath.substring(startPos), "/");

    int delimiterStart = StringUtils.indexOf(relative, rst.getSrcSubDir());
    if (delimiterStart == -1) {
      throw new StringIndexOutOfBoundsException();
    }
    int delimiterEnd = relative.indexOf("/", delimiterStart);
    String datasetName = StringUtils.removeEnd(relative.substring(0, delimiterStart), "/");
    String timeString = StringUtils.removeEnd(relative.substring(delimiterEnd + 1), "/");
    rst.datasetName = datasetName;
    rst.timeString = timeString;
    rst.time = getTime (timeString);
  }

  private DateTime getTime (String timeString) {
    DateTimeZone timeZone = DateTimeZone.forID(MRCompactor.DEFAULT_COMPACTION_TIMEZONE);
    int splits = StringUtils.countMatches(timeString, "/");
    String timePattern = "";
    if (splits == 3) {
      timePattern = "YYYY/MM/dd/HH";
    } else if (splits == 2) {
      timePattern = "YYYY/MM/dd";
    }
    DateTimeFormatter timeFormatter = DateTimeFormat.forPattern(timePattern).withZone(timeZone);
    return timeFormatter.parseDateTime (timeString);
  }

  private String getSrcBaseDir(State state) {
    Preconditions.checkArgument(state.contains(MRCompactor.COMPACTION_INPUT_DIR),
        "Missing required property " + MRCompactor.COMPACTION_INPUT_DIR);
    return state.getProp(MRCompactor.COMPACTION_INPUT_DIR);
  }

  private String getSrcSubDir(State state) {
    Preconditions.checkArgument(state.contains(MRCompactor.COMPACTION_INPUT_SUBDIR),
        "Missing required property " + MRCompactor.COMPACTION_INPUT_SUBDIR);
    return state.getProp(MRCompactor.COMPACTION_INPUT_SUBDIR);
  }

  private String getDstBaseDir(State state) {
    Preconditions.checkArgument(state.contains(MRCompactor.COMPACTION_DEST_DIR),
        "Missing required property " + MRCompactor.COMPACTION_DEST_DIR);
    return state.getProp(MRCompactor.COMPACTION_DEST_DIR);
  }

  private String getDstSubDir(State state) {
    Preconditions.checkArgument(state.contains(MRCompactor.COMPACTION_DEST_SUBDIR),
        "Missing required property " + MRCompactor.COMPACTION_DEST_SUBDIR);
    return state.getProp(MRCompactor.COMPACTION_DEST_SUBDIR);
  }
}
