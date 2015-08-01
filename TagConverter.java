import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import de.danielluedecke.zettelkasten.database.Daten;

public class TagConverter {

	private HashMap<String, String> authors = new HashMap<>();

	public TagConverter(HashMap<String, String> authors) {
		this.authors = authors;
	}

	private String replaceLiteratureFootnote(String text) {

		Pattern pattern = Pattern.compile(Pattern.quote("[fn ") + "(\\d+)"
				+ Pattern.quote("]") + "(,? ?\\w?. ?\\d+\\w?)?");

		for (Matcher matcher = pattern.matcher(text); matcher.find(); matcher = pattern
				.matcher(text)) {
			String fnMatch = text.substring(matcher.start(1), matcher.end(1))
					.trim();

			// Try to get page info
			String pMatch = "";
			try {
				pMatch = text.substring(matcher.start(2), matcher.end(2));
			} catch (Exception e) {

			}

			text = text.substring(0, matcher.start()) + "["
					+ authors.get(fnMatch) + pMatch + "](x-bdsk://"
					+ authors.get(fnMatch) + ")"
					+ text.substring(matcher.end(), text.length());
		}

		return text;
	}

	public String replaceNewline(String text) {
		return text.replaceAll("\\[br\\]", "\n");
	}

	@Test
	public void lala() {
		System.out
				.println(replaceLiteratureFootnote("In 2006, Berners-Lee introduced the term Linked Data with four optional but desirable rules for releasing data in the Web [fn 286], p. 5. Just like the previously introduced Semantic Web architecture, these rules contain W3C standards - a fact that is subject of critism [fn 21] - but they"));
	}

	public String replaceAll(String text) {
		text = replaceTables(text);
		text = replaceCites(text);
		text = replaceLiteratureFootnote(text);
		text = replaceNumberedList(text);
		text = replaceHeadlines(text);
		text = replaceSimpleFormats(text);
		text = replaceQuotes(text);
		text = replaceNewline(text);
		text = replaceImages(text);
		text = replaceBrokenSymbols(text);
		text = replaceLists(text);
		text = replaceUglyConstructs(text);
		text = makeColorsBold(text);
		return text;
	}

	private String makeColorsBold(String text) {
		text = text.replaceAll("\\[color #\\w+\\]", "**");
		text = text.replaceAll("\\[/color]", "**");
		return text;
	}

	private String replaceUglyConstructs(String text) {
		// Have two newslines at max
		text = text.replaceAll("\\n\\n\\n+", "\n\n\n");
		return text;
	}

	private String replaceLists(String text) {
		text = text.replaceAll(quoteTag("_"), "\n- ");
		text = text.replaceAll(quoteTag("/_"), "");
		text = text.replaceAll(quoteTag("l"), "");
		text = text.replaceAll(quoteTag("/l"), "");
		return text;
	}

	private String replaceBrokenSymbols(String text) {
		// &#9;
		text = text.replaceAll("&#9;", "");
		return text;
	}

	private String replaceTables(String text) {
		Pattern pattern = Pattern.compile(Pattern.quote("[table]") + "(.+?)"
				+ Pattern.quote("[/table]"));

		for (Matcher matcher = pattern.matcher(text); matcher.find(); matcher = pattern
				.matcher(text)) {

			String tableContent = text
					.substring(matcher.start(), matcher.end());
			tableContent = tableContent.substring(7, tableContent.length() - 8);

			// Extract Table Description, if possible
			String tableDescription = "";
			Matcher tcMatcher = Pattern.compile(
					Pattern.quote("[tc]") + "(.+?)" + Pattern.quote("[/tc]"))
					.matcher(tableContent);
			if (tcMatcher.find()) {
				tableDescription = tableContent.substring(tcMatcher.start(),
						tcMatcher.end());
				tableDescription = tableDescription.substring(4,
						tableDescription.length() - 5);

				// remove it
				tableContent = tableContent.substring(0, tcMatcher.start())
						+ tableContent.substring(tcMatcher.end(),
								tableContent.length());
			}

			// System.out.println("HEAD: " + tableDescription);

			tableContent = tableContent.replaceAll(quoteTag("br"), "\n");
			// System.out.println("T:" + tableContent + ":T");

			// Scan for number of columns, try with first 5 rows
			int numOfHeadlineMarkers = 0;
			int numOfColMarker = 0;
			for (int i = 0; i <= 5; i++) {
				try {
					int thisHeadLineMarker = tableContent.split("\n")[i]
							.split("\\^").length;

					if (thisHeadLineMarker > numOfHeadlineMarkers)
						numOfHeadlineMarkers = thisHeadLineMarker;

					int thisColumnMarker = tableContent.split("\n")[i]
							.split("\\|").length;
					if (thisColumnMarker > numOfColMarker)
						numOfColMarker = thisColumnMarker;
				} catch (IndexOutOfBoundsException e) {
					// Ignore that, will fall back automatically
				}
			}

			int columns = numOfHeadlineMarkers > 1 ? numOfHeadlineMarkers
					: numOfColMarker;

			// Construct table divider row
			String headRow = "";
			String divRow = "";
			for (int i = 0; i < columns; i++) {
				divRow = divRow + "---";
				divRow += " | ";
				headRow += " |";
			}

			if (numOfHeadlineMarkers > 1) {
				String[] rows = tableContent.split("\\n");
				tableContent = "";
				boolean dividerRowWritten = false;
				for (int i = 0; i < rows.length; i++) {
					String row = rows[i].trim();

					if (row.equals(""))
						continue;

					tableContent += " " + row + "\n";

					if (!dividerRowWritten) {
						tableContent += divRow + "\n";
						dividerRowWritten = true;
					}
				}
			} else {
				// Table had no headline row
				tableContent = headRow + "\n" + divRow + "\n" + tableContent;
			}

			tableContent = tableContent.replaceAll("\\n\\n", "\n");

			tableContent = tableContent.replaceAll("\\^", "|");
			// String absoluteImagePath = daten.getUserImagePath()
			// .getAbsolutePath() + "/" + relativeImagePath;

			text = text.substring(0, matcher.start())
					+ "\n\n"
					+ (!tableDescription.equals("") ? "# " + tableDescription
							+ "\n\n" : "") + tableContent + "\n\n"
					+ text.substring(matcher.end(), text.length());
		}

		return text;
	}

	private String replaceImages(String text) {
		// text = text.replaceAll(quoteTag("img"), "![](");
		// text = text.replaceAll(quoteTag("/img"), ")");
		//
		Pattern pattern = Pattern.compile(Pattern.quote("[img]") + "(.+?)"
				+ Pattern.quote("[/img]"));

		for (Matcher matcher = pattern.matcher(text); matcher.find(); matcher = pattern
				.matcher(text)) {

			String relativeImagePath = text.substring(matcher.start(),
					matcher.end());
			relativeImagePath = relativeImagePath.substring(5,
					relativeImagePath.length() - 6);

			// String absoluteImagePath = daten.getUserImagePath()
			// .getAbsolutePath() + "/" + relativeImagePath;

			relativeImagePath = relativeImagePath.replaceAll(" ", "%20");

			text = text.substring(0, matcher.start()) + "![](" + "img/"
					+ relativeImagePath + ")"
					+ text.substring(matcher.end(), text.length());
		}

		return text;
	}

	private String replaceCites(String text) {

		// Pattern pattern = Pattern.compile(Pattern.quote("[q]")+ "(.*)"+
		// Pattern.quote("[/q]"));
		Pattern pattern = Pattern.compile(Pattern.quote("[q]") + "(.+?)"
				+ Pattern.quote("[/q]"));

		for (Matcher matcher = pattern.matcher(text); matcher.find(); matcher = pattern
				.matcher(text)) {

			String textWithinQ = text.substring(matcher.start(), matcher.end());

			// Special [br] convertion when within a [q]
			textWithinQ = textWithinQ.replaceAll("\\[br\\]", "\n>");

			text = text.substring(0, matcher.start()) + "\n> "
					+ textWithinQ.substring(3, textWithinQ.length() - 4) + "\n"
					+ text.substring(matcher.end(), text.length());
		}

		text = text.replaceAll(quoteTag("q"), "> ");
		text = text.replaceAll(quoteTag("/q"), "\n\n");
		return text;
	}

	private String replaceQuotes(String text) {
		text = text.replaceAll(quoteTag("qm"), "\"");
		text = text.replaceAll(quoteTag("/qm"), "\"");
		return text;
	}

	private String replaceSimpleFormats(String text) {
		text = text.replaceAll(quoteTag("k"), "_");
		text = text.replaceAll(quoteTag("/k"), "_");
		text = text.replaceAll(quoteTag("f"), "**");
		text = text.replaceAll(quoteTag("/f"), "**");
		return text;
	}

	private String replaceHeadlines(String text) {
		text = text.replaceAll(quoteTag("h1"), "# ");
		text = text.replaceAll(quoteTag("/h1"), "");
		text = text.replaceAll(quoteTag("h2"), "## ");
		text = text.replaceAll(quoteTag("/h2"), "");
		text = text.replaceAll(quoteTag("h3"), "### ");
		text = text.replaceAll(quoteTag("/h3"), "");
		return text;
	}

	private String replaceNumberedList(String text) {
		text = text.replaceAll("\\[\\*\\]", "\n1. ");
		text = text.replaceAll("\\[/\\*\\]", "");
		text = text.replaceAll("\\[n\\]", "");
		text = text.replaceAll("\\[/n\\]", "");
		return text;
	}

	private String quoteTag(String tag) {
		return "\\[" + tag + "\\]";
	}
}
