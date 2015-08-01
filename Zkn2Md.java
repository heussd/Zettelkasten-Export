import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;

import org.jdesktop.application.SingleFrameApplication;
import org.springframework.web.util.UriUtils;

import de.danielluedecke.zettelkasten.ZettelkastenView;
import de.danielluedecke.zettelkasten.database.AcceleratorKeys;
import de.danielluedecke.zettelkasten.database.AutoKorrektur;
import de.danielluedecke.zettelkasten.database.Daten;
import de.danielluedecke.zettelkasten.database.Settings;
import de.danielluedecke.zettelkasten.database.StenoData;
import de.danielluedecke.zettelkasten.database.Synonyms;
import de.danielluedecke.zettelkasten.database.TasksData;

public class Zkn2Md {

	public void lala() throws Exception {
		String file = new String("/Users/zkn/Zettelwirtschaft.zkn3");
		PrintWriter pw = new PrintWriter(new FileWriter("out.txt"));

		StenoData stenoData = null;
		AutoKorrektur autoKorrektur = null;
		TasksData tasksData = null;
		AcceleratorKeys acceleratorKeys = new AcceleratorKeys();
		Synonyms synonyms = new Synonyms();

		Settings settings = new Settings(acceleratorKeys, autoKorrektur,
				synonyms, stenoData);
		// File fileObject = new File(this.getClass().getClassLoader()
		// .getResource(file).getPath());
		settings.setFilePath(new File(file));

		ZettelkastenView zettelkastenView = new ZettelkastenView(
				new SingleFrameApplication() {
					@Override
					protected void startup() {
					}
				}, settings, acceleratorKeys, autoKorrektur, synonyms,
				stenoData, tasksData);

		Field field = zettelkastenView.getClass().getDeclaredField("data");
		field.setAccessible(true);

		Daten daten = (Daten) field.get(zettelkastenView);

		// gelöschte

		// Create Hashmap with prefered author information
		HashMap<String, String> authors = new HashMap<>();
		HashMap<String, String> authorsReverseIndex = new HashMap<>();
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			if (daten.getAuthor(i).equals(""))
				break;

			String bibkey = daten.getAuthorBibKey(i);
			String authorString = daten.getAuthor(i);

			String reference = null;
			reference = bibkey == null ? authorString
					: bibkey.equals("") ? authorString : bibkey;

			authors.put(i + "", reference);
			authorsReverseIndex.put(authorString, i + "");
		}

		TagConverter tagConverter = new TagConverter(authors);

		for (int i = 1; i <= 2103; i++) {
			String content = daten.getZettelContent(i);
			// System.out.println(content);
			String title = daten.getZettelTitle(i);

			// daten.get

			// System.out.println();

			String keywords = "";

			if (daten.getKeywords(i) != null) {
				for (String keyword : daten.getKeywords(i)) {
					if (keyword.startsWith("#")) {
						keywords += (keyword.substring(1)).replaceAll(" ", "")
								+ ",";
						// System.out.println(keywords);
					}
				}
			}

			String zettelcontent = tagConverter.replaceAll(content);

			// Make sure author's information are taken with the export,
			// non-referred Authors are likely to appear exclusively here...
			if (daten.getAuthors(i) != null) {
				zettelcontent += "\n\n\nAssociated Authors: ";

				for (String author : daten.getAuthors(i)) {
					String authorStatement = authors.get(daten
							.getAuthorID(author));
					if (authorStatement == null) {
						authorStatement = authors.get(authorsReverseIndex
								.get(author));
					}

					zettelcontent += "[" + authorStatement + "](x-bdsk://"
							+ authorStatement + ")\n";
				}
			}

			zettelcontent += "\n\n\nMigrated from Zettel " + i;

			// System.out.println(zettelcontent);
			
			String line = "open -g \"nv://make?txt="
					+ encode(zettelcontent) + "&title=" + encode(title)
					+ "&tags=" + encode(keywords) + "\"";
			
			
			System.out.println(line);
			pw.write(line + "\n");
			System.out.println(i);

		}
		
		pw.close();
		//
		//
		// // daten.getZettelContent(pos)
		// // daten.getAuthorD
		//
		// System.out.println(daten.getAuthorBibKey(286));
	}

	// public boolean fireGet(String zettelContent) {
	// // https://github.com/scrod/nv/wiki/nv%3A---URL-Specification
	// //
	// nv://make/?title=URI-escaped-title&html=URI-escaped-HTML-data&tags=URI-escaped-tag-string
	// try {
	// Runtime rt = Runtime.getRuntime();
	// rt.exec("/usr/bin/open -g 'nv://make?txt=LALALALLALAL'");
	// // System.out.println("Process exited with code = " + p.exitValue());
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return false;
	// }

	private String encode(String content) throws Exception {
		// http://stackoverflow.com/questions/14321873/java-url-encoding-urlencoder-vs-uri
		return UriUtils.encodeQueryParam(content, "UTF-8").replaceAll("\\!", "%21");
		// .replaceAll("&",
		// "%26");
		// content = URLEncoder.encode((content), "UTF-8")
		// .replaceAll("\\+", "%20").replaceAll("\\%21", "!")
		// .replaceAll("\\%27", "'").replaceAll("\\%28", "(")
		// .replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
		// return content;
	}

	public static void main(String[] args) throws Exception {
		new Zkn2Md().lala();
	}
}
