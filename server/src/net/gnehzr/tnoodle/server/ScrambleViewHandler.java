package net.gnehzr.tnoodle.server;

import static net.gnehzr.tnoodle.utils.Utils.GSON;
import static net.gnehzr.tnoodle.utils.Utils.parseExtension;
import static net.gnehzr.tnoodle.utils.Utils.throwableToString;
import static net.gnehzr.tnoodle.utils.Utils.toInt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.SortedMap;

import javax.imageio.ImageIO;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;
import net.gnehzr.tnoodle.scrambles.Scrambler;

import com.sun.net.httpserver.HttpExchange;

public class ScrambleViewHandler extends SafeHttpHandler {
	private SortedMap<String, Scrambler> scramblers;
	public ScrambleViewHandler() {
		this.scramblers = Scrambler.getScramblers();
	}
	
	protected void wrappedHandle(HttpExchange t, String path[], HashMap<String, String> query) throws IOException {
		String callback = query.get("callback");
		if(path.length == 1) {
			sendJSONError(t, "Please specify a puzzle.", callback);
			return;
		}
		String[] name_extension = parseExtension(path[1]);
		if(name_extension[1] == null) {
			sendJSONError(t, "Please specify an extension", callback);
			return;
		}
		String puzzle = name_extension[0];
		String extension = name_extension[1];
		
		Scrambler scrambler = scramblers.get(puzzle);
		if(scrambler == null) {
			sendJSONError(t, "Invalid scrambler: " + puzzle, callback);
			return;
		}

		HashMap<String, Color> colorScheme = scrambler.parseColorScheme(query.get("scheme"));
		
		String scramble = query.get("scramble");
		Dimension dimension = scrambler.getPreferredSize(toInt(query.get("width"), 0), toInt(query.get("height"), 0));
		if(extension.equals("png")) {
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				if(query.containsKey("icon")) {
					scrambler.loadPuzzleIcon(bytes);
				} else {
					BufferedImage img = new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB);
					scrambler.drawScramble(img.createGraphics(), dimension, scramble, colorScheme);
					ImageIO.write(img, "png", bytes);
				}

				t.getResponseHeaders().set("Content-Type", "image/png");
				t.sendResponseHeaders(200, bytes.size());
				t.getResponseBody().write(bytes.toByteArray());
				t.getResponseBody().close();
			} catch(InvalidScrambleException e) {
				e.printStackTrace();
				sendText(t, throwableToString(e));
			}
		} else if(extension.equals("json")) {
			sendJSON(t, GSON.toJson(scrambler.getDefaultPuzzleImageInfo().jsonize()), callback);
		} else {
			sendJSONError(t, "Invalid extension: " + extension, callback);
		}
	}
}
