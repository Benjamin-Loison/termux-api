package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.PrintWriter;

import com.termux.api.TermuxAccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.StringWriter;

import android.graphics.Rect;

import android.content.ContentResolver;

public class AccessibilityAPI {

    private static final String LOG_TAG = "AccessibilityAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

		ResultReturner.returnData(apiReceiver, intent, out -> {
			final ContentResolver contentResolver = context.getContentResolver();
			if (intent.hasExtra("dump")) {
				out.print(dump());
			}
			else if (intent.hasExtra("click")) {
				click(intent.getIntExtra("x", 0), intent.getIntExtra("y", 0));
			}
		});
	}

	private static void click(int x, int y) {
		Path swipePath = new Path();
	    swipePath.moveTo(x, y);
	    GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
	    gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 1));
	    TermuxAccessibilityService.instance.dispatchGesture(gestureBuilder.build(), null, null);
	}

	// The aim of this function is to give a compatible output with `adb` `uiautomator dump`.
	private static String dump() throws TransformerException, ParserConfigurationException {
		// Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("hierarchy");
        document.appendChild(root);

		AccessibilityNodeInfo node = TermuxAccessibilityService.instance.getRootInActiveWindow();

		dumpNodeAuxiliary(document, root, node);

        // Write as XML
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(document);

		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(source, result);

		return sw.toString();
	}

	private static void dumpNodeAuxiliary(Document document, Element element, AccessibilityNodeInfo node) {
		for (int i = 0; i < node.getChildCount(); i++) {
			AccessibilityNodeInfo nodeChild = node.getChild(i);
			Element elementChild = document.createElement("node");
			
			elementChild.setAttribute("index", String.valueOf(i));

			elementChild.setAttribute("text", getCharSequenceAsString(nodeChild.getText()));

			String nodeChildViewIdResourceName = nodeChild.getViewIdResourceName();
			elementChild.setAttribute("resource-id", nodeChildViewIdResourceName != null ? nodeChildViewIdResourceName : "");
			
			elementChild.setAttribute("class", nodeChild.getClassName().toString());
			
			elementChild.setAttribute("package", nodeChild.getPackageName().toString());

			elementChild.setAttribute("content-desc", getCharSequenceAsString(nodeChild.getContentDescription()));
			
			elementChild.setAttribute("checkable", String.valueOf(nodeChild.isCheckable()));
			
			elementChild.setAttribute("checked", String.valueOf(nodeChild.isChecked()));
			
			elementChild.setAttribute("clickable", String.valueOf(nodeChild.isClickable()));
			
			elementChild.setAttribute("enabled", String.valueOf(nodeChild.isEnabled()));
			
			elementChild.setAttribute("focusable", String.valueOf(nodeChild.isFocusable()));
			
			elementChild.setAttribute("focused", String.valueOf(nodeChild.isFocused()));
			
			elementChild.setAttribute("scrollable", String.valueOf(nodeChild.isScrollable()));
			
			elementChild.setAttribute("long-clickable", String.valueOf(nodeChild.isLongClickable()));
			
			elementChild.setAttribute("password", String.valueOf(nodeChild.isPassword()));
			
			elementChild.setAttribute("selected", String.valueOf(nodeChild.isSelected()));
			
			Rect nodeChildBounds = new Rect();
			nodeChild.getBoundsInScreen(nodeChildBounds);
			elementChild.setAttribute("bounds", nodeChildBounds.toShortString());
			
			elementChild.setAttribute("drawing-order", String.valueOf(nodeChild.getDrawingOrder()));
			
			elementChild.setAttribute("hint", getCharSequenceAsString(nodeChild.getHintText()));
			
			element.appendChild(elementChild);
            dumpNodeAuxiliary(document, elementChild, nodeChild);
        }
	}

	private static String getCharSequenceAsString(CharSequence charSequence) {
		return charSequence != null ? charSequence.toString() : "";
	}
}
