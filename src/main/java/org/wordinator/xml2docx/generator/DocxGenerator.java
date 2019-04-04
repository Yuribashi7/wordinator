/**
 * 
 */
package org.wordinator.xml2docx.generator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.util.SystemOutLogger;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.common.usermodel.fonts.FontFamily;
import org.apache.poi.sl.draw.DrawShape;
import org.apache.poi.sl.draw.DrawSimpleShape;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFAbstractFootnoteEndnote;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlCursor.TokenType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalAlignRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.impl.STOnOffImpl;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;



/**
 * Generates DOCX files from Simple Word Processing Markup Language XML.
 */
@SuppressWarnings("unused")
public class DocxGenerator {
	public static final Logger log = LogManager.getLogger();

	private File outFile;
	private int dotsPerInch = 72; /* DPI */
	// Map of source IDs to internal object IDs.
	private Map<String, BigInteger> bookmarkIdToIdMap = new HashMap<String, BigInteger>();
	private int idCtr = 0;
	private File inFile;
	private XWPFDocument templateDoc;

	/**
	 * 
	 * @param inFile File representing input document.
	 * @param outFile File to write DOCX result to
	 * @param templateDoc DOTX template to initialize result DOCX with (provides style definitions)
	 * @throws Exception Exception from loading the template document
	 * @throws FileNotFoundException If the template document is not found
	 */
	public DocxGenerator(File inFile, File outFile, XWPFDocument templateDoc) throws FileNotFoundException, Exception {
		this.inFile = inFile;
		this.outFile = outFile;		
		this.templateDoc = templateDoc;
	}

	/*
	 * Generate the DOCX file from the input Simple WP ML document. 
	 * @param xml The XmlObject that holds the Simple WP XML content
	 */
	public void generate(XmlObject xml) throws DocxGenerationException, XmlException, IOException {
		
		XWPFDocument doc = new XWPFDocument();
		
		setupStyles(doc, this.templateDoc);
		constructDoc(doc, xml);
				
		FileOutputStream out = new FileOutputStream(outFile);
        doc.write(out);
        doc.enforceUpdateFields();
		doc.close(); 
	}

	/**
	 * Walk the XML document to create the Word document.
	 * @param doc Word document to write to
	 * @param xml Simple ML doc to walk
	 */
	private void constructDoc(XWPFDocument doc, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		cursor.toFirstChild(); // Put us on the root element of the document
		cursor.push();
		
		if (cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "page-sequence-properties"))) {
			setupPageSequence(doc, cursor.getObject());
		}
		
		cursor.pop();
		cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "body"));
		handleBody(doc, cursor.getObject());			
	}

	/**
	 * Process the elements in &lt;body&gt;
	 * @param doc Document to add paragraphs to.
	 * @param xml Body element
	 * @throws DocxGenerationException
	 */
	private void handleBody(XWPFDocument doc, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		if (cursor.toFirstChild()) {
			do {
				
				String tagName = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();
				
				if ("p".equals(tagName)) {
					XWPFParagraph p = doc.createParagraph();
					makeParagraph(p, cursor, "body");
				} else if ("section".equals(tagName)) {
					handleSection(doc, cursor.getObject());
				} else if ("table".equals(tagName)) {
					XWPFTable table = doc.createTable();
					makeTable(table, cursor.getObject());
				} else if ("object".equals(tagName)) {
					// FIXME: This is currently unimplemented.
					makeObject(doc, cursor);
				} else {
					log.warn("handleBody(): Unexpected element {" + namespace + "}:'" + tagName + "' in <body>. Ignored.");
				}
			} while (cursor.toNextSibling());
		}
	}

	/**
	 * Handle a &lt;section&gt; element
	 * @param doc Document we're adding to
	 * @param xml &lt;section&gt; element
	 */
	@SuppressWarnings("unused")
	private void handleSection(XWPFDocument doc, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		
		log.warn("Section-level headers and footers and page numbering not yet implemented.");
		// FIXME: The section-specific properties go in the first paragraph of the section.
		cursor.push();
		if (false && cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "page-sequence-properties"))) {
			setupPageSequence(doc, cursor.getObject());
		}
		cursor.pop();
		
		cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "body"));
		handleBody(doc, cursor.getObject());
		
	}

	/**
	 * Set up page sequence properties for the entire document, including page geometry, numbering, and headers and footers.
	 * @param doc Document to be constructed
	 * @param xml page-sequence-properties element
	 * @throws DocxGenerationException 
	 */
	private void setupPageSequence(XWPFDocument doc, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		cursor.push();
		if (cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "page-number-properties"))) {
			String format = cursor.getAttributeText(DocxConstants.QNAME_FORMAT_ATT);
			if (null != format) {
				// FIXME: Not sure how to set this up with the POI API yet.
			}
		}
		cursor.pop();
		cursor.push();
		if (cursor.toChild(new QName(DocxConstants.SIMPLE_WP_NS, "headers-and-footers"))) {
			constructHeadersAndFooters(doc, cursor.getObject());
		}
		cursor.pop();
		
	}

	/**
	 * Construct headers and footers for the document.
	 * @param doc Document to add headers and footers to.
	 * @param xml headers-and-footers element
	 * @throws DocxGenerationException 
	 */
	private void constructHeadersAndFooters(XWPFDocument doc, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		
		boolean haveOddHeader = false;
		boolean haveEvenHeader = false;
		boolean haveOddFooter = false;
		boolean haveEvenFooter = false;
		
		if (cursor.toFirstChild()) {
			do {
				String tagName = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();
				if ("header".equals(tagName)) {
					HeaderFooterType type = getHeaderFooterType(cursor);
					if (type == HeaderFooterType.DEFAULT) {
						haveOddHeader = true;
					}
					if (type == HeaderFooterType.EVEN) {
						haveEvenHeader = true;
					}
					XWPFHeader header = doc.createHeader(type);
					makeHeaderFooter(header, cursor.getObject());
				} else if ("footer".equals(tagName)) {
					HeaderFooterType type = getHeaderFooterType(cursor);
					if (type == HeaderFooterType.DEFAULT) {
						haveOddFooter = true;
					}
					if (type == HeaderFooterType.EVEN) {
						haveEvenFooter = true;
					}
					XWPFFooter footer = doc.createFooter(type);
					makeHeaderFooter(footer, cursor.getObject());
				} else {
					log.warn("Unexpected element {" + namespace + "}:" + tagName + " in <headers-and-footers>. Ignored.");
				}
			} while(cursor.toNextSibling());
		}
		
		if ((haveOddHeader || haveOddFooter) && 
			(haveEvenHeader || haveEvenFooter)) {
			doc.setEvenAndOddHeadings(true);
		}
		
	}

	/**
	 * Construct the content of a page header or footer
	 * @param headerFooter {@link XPWFHeader} or {@link XWPFFooter} to add content to
	 * @param xml The &lt;header&gt; or &lt;footer&gt; element to process
	 * @throws DocxGenerationException 
	 */
	private void makeHeaderFooter(XWPFHeaderFooter headerFooter, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();

		String myparent;
		
		if(headerFooter.toString().contains("/word/footer1.xml")) {
			myparent = "footer";
		} else {
			myparent = "header";
		}
		if (cursor.toFirstChild()) {
			do {
				String tagName = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();
				if ("p".equals(tagName)) {
					XWPFParagraph p = headerFooter.createParagraph();					
					makeParagraph(p, cursor, myparent);
				} else if ("table".equals(tagName)) {
					XWPFTable table = headerFooter.createTable(0, 0);
					makeTable(table, cursor.getObject());
				} else {
					// There are other body-level things that could go in a footnote but 
					// we aren't worrying about them for now.
					log.warn("makeFootnote(): Unexpected element {" + namespace + "}:" + tagName + "' in <fn>. Ignored.");
				}
			} while(cursor.toNextSibling());
		}
	}

	/**
	 * Get the header or footer type for the element at the cursor.
	 * @param cursor
	 * @return {@link HeaderFooterType}
	 */
	private HeaderFooterType getHeaderFooterType(XmlCursor cursor) {
		HeaderFooterType type = HeaderFooterType.DEFAULT;
		String typeName = cursor.getAttributeText(DocxConstants.QNAME_TYPE_ATT);
		if ("even".equals(typeName)) {
			type = HeaderFooterType.EVEN;
		} 
		if ("first".equals(typeName)) {
			type = HeaderFooterType.FIRST;
		}
		return type;
	}

	/**
	 * Construct a Word paragraph
	 * @param para The Word paragraph to construct
	 * @param cursor Cursor pointing at the <p> element the paragraph will reflect.
	 * @return Paragraph (should be same object as passed in).
	 */
	private XWPFParagraph makeParagraph(XWPFParagraph para, XmlCursor cursor, String parent) throws DocxGenerationException {		
		cursor.push();
		String styleName = cursor.getAttributeText(DocxConstants.QNAME_STYLE_ATT);
		String styleId = cursor.getAttributeText(DocxConstants.QNAME_STYLEID_ATT);
		
		if(parent.equals("FootnoteText")) {
			para.setStyle("FootnoteText");
		} else if(parent.equals("header")) {
			para.setStyle("Header");		
		} else if(parent.equals("footer")) {
			para.setStyle("Footer");			
		} else {
			if (null != styleName && null == styleId) {
				// Look up the style by name:
				XWPFStyle style = para.getDocument().getStyles().getStyleWithName(styleName);
				if (null != style) {
					styleId = style.getStyleId();
				}
			}
			
			if (null != styleId) {
				para.setStyle(styleId);
			}
		}
		
		if (cursor.toFirstChild()) {
			do {
				String tagName = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();

				if ("run".equals(tagName)) {					
					makeRun(para, cursor.getObject());
				} else if ("bookmarkStart".equals(tagName)) {
					makeBookmarkStart(para, cursor);
				} else if ("bookmarkEnd".equals(tagName)) {
					makeBookmarkEnd(para, cursor);
				} else if ("fn".equals(tagName)) {
					makeFootnote(para, cursor);
				} else if ("hyperlink".equals(tagName)) {
					makeHyperlink(para, cursor);
				} else if ("image".equals(tagName)) {
					makeImage(para, cursor);
				} else if ("object".equals(tagName)) {
					makeObject(para, cursor);
				} else if ("page-number-ref".equals(tagName)) {
					makePageNumberRef(para, cursor);
				} else if ("rule".equals(tagName)) {
					makeRule(para, cursor);									
				} else if ("minitoc".equals(tagName)) {					
					if(inFile.getName().toString().startsWith("^")) {
						buildMiniToc(para, cursor);	
					}					
				} else {
					log.warn("Unexpected element {" + namespace + "}:" + tagName + " in <p>. Ignored.");
				}
			} while(cursor.toNextSibling());
		}
		
		cursor.pop();
		return para;
	}
	

	private void doCR(XWPFParagraph para, XmlCursor cursor) {
		XWPFRun run=para.createRun();		
		//run.addCarriageReturn();
	}
	
	
	/*
	 * NOTICE: The following is kind-of-a hack. Knowing that the number of '_' set to 14 per inch is arbitrary.
	 * It's based on the 'normal' font and size allowing for 14 underscore characters per inch.
	 */
	private void makeRule(XWPFParagraph para, XmlCursor cursor) {	

//		System.out.println("BEGINNING: makeRule and have fun!");
		
		org.wordinator.xml2docx.generator.Measurement Measurement = new org.wordinator.xml2docx.generator.Measurement();
		XWPFRun run=para.createRun();		
				
		// ruleWeightRaw...
		cursor.push();
		String ruleWeightRaw = cursor.getAttributeText(DocxConstants.QNAME_RULEWEIGHT_ATT);	
		if (null == ruleWeightRaw) {
			log.error("- [ERROR] No @weight attribute for rule.");
			return;
		}
		cursor.pop();
		
		// ruleWeightUnitsRaw...
		cursor.push();
		String ruleWeightUnitsRaw = cursor.getAttributeText(DocxConstants.QNAME_RULEWEIGHTUNITS_ATT);
		// xpp_measure_units: 
		// centimeters, points, picas, ciceros, didots, inches, millimeters, kyus, microns, units

		switch (ruleWeightUnitsRaw) {
        	case "inches":	ruleWeightUnitsRaw = "in";
                 break;
        	case "points":	ruleWeightUnitsRaw = "pt";
            	break;
        	case "picas":	ruleWeightUnitsRaw = "pc";
        		break;
        	default:		ruleWeightUnitsRaw = "pt";
		}
		
		if (null == ruleWeightUnitsRaw) {
			log.error("- [ERROR] No @weight_units attribute for rule.");
			return;
		}
		cursor.pop();
		
		// ruleWidthRaw...
		cursor.push();
		String ruleWidthRaw = cursor.getAttributeText(DocxConstants.QNAME_RULEWIDTH_ATT);
		
		if (null == ruleWidthRaw) {
			log.error("- [ERROR] No @width attribute for rule.");
			return;
		}
		cursor.pop();
		
		// ruleWidthUnitsRaw...
		cursor.push();
		String ruleWidthUnitsRaw = cursor.getAttributeText(DocxConstants.QNAME_RULEWIDTHUNITS_ATT);
		
		switch (ruleWidthUnitsRaw) {
	    	case "inches":	ruleWidthUnitsRaw = "in";
	             break;
	    	case "points":	ruleWidthUnitsRaw = "pt";
	        	break;
	    	case "picas":	ruleWidthUnitsRaw = "pc";
	    		break;
	    	default:		ruleWidthUnitsRaw = "pt";
		}
		if (null == ruleWidthUnitsRaw) {
			log.error("- [ERROR] No @width_units attribute for rule.");
			return;
		}
		cursor.pop();
		
//		log.info("makeRule:       @weight: [" + ruleWeightRaw + "]");
//		log.info("makeRule: @weight_units: [" + ruleWeightUnitsRaw + "]");
//		log.info("makeRule:        @width: [" + ruleWidthRaw + "]");
//		log.info("makeRule:  @width_units: [" + ruleWidthUnitsRaw + "]");
		
		try {
			@SuppressWarnings("static-access")
			double ruleWeight = Measurement.toPoints(ruleWeightRaw + ruleWeightUnitsRaw, getDotsPerInch());
//			System.out.println("[toPoints] ruleWeight: " + ruleWeight + " [from: " + ruleWeightRaw + " dpi:" + dotsPerInch + "]");
		} catch (MeasurementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			@SuppressWarnings("static-access")
			double ruleWidth = Measurement.toInches(ruleWidthRaw + ruleWidthUnitsRaw, getDotsPerInch());
//			System.out.println("[toInches] ruleWidth: " + ruleWidth + " [from: " + ruleWidthRaw + " dpi:" + dotsPerInch + "]");
			
			run.setFontFamily("Swiss");
			int repeats = (int) (ruleWidth * 14);	// about 14 per inch
			
			String str1 = "_";	// emspace
			StringBuffer buffer = new StringBuffer(str1);
			for (int i = 0; i < repeats; i++) {
			    buffer.append(str1);
			}	
			
			run.setText(buffer.toString());
			
		} catch (MeasurementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void buildDateTimeStuff(XWPFParagraph para, XmlCursor cursor) {
		XWPFRun run=para.createRun();

		ZoneId zoneId = ZoneId.of("America/New_York");
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime nowZone = LocalDateTime.now(zoneId);
		run.setText("   (Created: " + nowZone.toString() + "[" + zoneId.toString() + "])");
		run.setFontFamily("Consolas");
		run.setFontSize(6);
	}
	
	
	private void makePageNumberRef(XWPFParagraph para, XmlCursor cursor) {
		// PAGE of NUMPAGES...
		XWPFRun run=para.createRun();		
		run.addCarriageReturn();
		para.setAlignment(ParagraphAlignment.CENTER);
	
		run = para.createRun();
		run.setText("Page ");
		para.getCTP().addNewFldSimple().setInstr("PAGE \\* MERGEFORMAT");
		run = para.createRun();  
		run.setText(" of ");
		para.getCTP().addNewFldSimple().setInstr("NUMPAGES \\* MERGEFORMAT");		
	}	
	
	
	private void buildMiniToc(XWPFParagraph para, XmlCursor cursor) {
		CTP ctP = para.getCTP();
		CTSimpleField toc = ctP.addNewFldSimple();
		toc.setInstr("TOC \\h");
		toc.setDirty(STOnOff.TRUE);
	}
	

	/**
	 * Construct a run within a paragraph.
	 * @param para The output paragraph to add the run to.
	 * @param xml The <run> element.
	 */
	private void makeRun(XWPFParagraph para, XmlObject xml) throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		String tagname = cursor.getName().getLocalPart(); // For debugging
//		System.out.println("[makeRun]:tagname:" + tagname);
		
		XWPFRun run = para.createRun();
		String styleName = cursor.getAttributeText(DocxConstants.QNAME_STYLE_ATT);
		String styleId = cursor.getAttributeText(DocxConstants.QNAME_STYLEID_ATT);	
		
		if (null != styleName && null == styleId) {
			// Look up the style by name:
			XWPFStyle style = para.getDocument().getStyles().getStyleWithName(styleName);
			if (null != style) {
				styleId = style.getStyleId();
			}
		}	
		
		if (null != styleId) {
			run.setStyle(styleId);
		}
		
		handleFormattingAttributes(run, xml);
		
		cursor.toLastAttribute();
		cursor.toNextToken(); // Should be first text or subelement.
		
		// In this loop, each different token handler is responsible for positioning
		// the cursor past the thing that was handled such that the only END token
		// is the end for the run element being processed.
		while (TokenType.END != cursor.currentTokenType()) {
			
//			TokenType tokenType = cursor.currentTokenType(); // For debugging
//			System.out.println("...tokenType: " + tokenType);			
//			System.out.println("...cursor:" + cursor.getTextValue());
			
			if (cursor.isText()) {
				run.setText(cursor.getTextValue());
				cursor.toNextToken();
			} else if (cursor.isAttr()) {
				// Ignore attributes in this context.
			} else if (cursor.isStart()) {
				// Handle element within run
				String name = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();	
				
				if ("break".equals(name)) {
					makeBreak(run, cursor);
					
				} else if ("doCR".equals(name)) {
					doCR(para, cursor);
					
				} else if ("dateTimeStuff".equals(name)) {					
					buildDateTimeStuff(para, cursor);
					
				} else if ("img".equals(name)) {
					makeImage(para, cursor);
					
				} else if ("symbol".equals(name)) {
					makeSymbol(run, cursor);
					
				} else if ("tab".equals(name)) {
					makeTab(run, cursor);
					
				} else {
					log.error("makeRun(); Unexpected element {" + namespace + "}:" + name + ". Skipping.");
					cursor.toEndToken(); // Skip this element.
				}
				cursor.toNextToken();
			} else if (cursor.isComment() || cursor.isProcinst()) {
				// Silently ignore
				// FIXME: Not sure if we need to do more to skip a comment or processing instruction.
				cursor.toNextToken();
			} else {
				// What else could there be?
				if (cursor.getName() != null) {
					log.error("makeRun(): Unhanded XML token " + cursor.getName().getLocalPart());
				} else {
					log.error("makeRun(): Unhanded XML token " + cursor.currentTokenType());
				}
				cursor.toNextToken();
			}
		} 
		
		cursor.pop();
	}
	
	private void handleFormattingAttributes(XWPFRun run, XmlObject xml) {
		XmlCursor cursor = xml.newCursor();
		if (cursor.toFirstAttribute()) {
			do {
				  String attName = cursor.getName().getLocalPart();
				  String attValue = cursor.getTextValue();
//				  System.out.println("......" + attName  + ":" + attValue);
				  
				  if ("bold".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setBold(value);
				  } else if ("caps".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setCapitalized(value);
				  } else if ("color".equals(attName)) {
					  // NOTE: color must be an RGB hex number. May need to translate
					  // from color names to RGB values.
				      run.setColor(attValue);
				  } else if ("double-strikethrough".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setDoubleStrikethrough(value);
				  } else if ("emboss".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setEmbossed(value);
				  } else if ("emphasis-mark".equals(attName)) {
				      run.setEmphasisMark(attValue);
				  } else if ("expand-collapse".equals(attName)) {
					  int percentage = Integer.valueOf(attValue);
					  run.setTextScale(percentage);
				  } else if ("highlight".equals(attName)) {
					  run.setTextHighlightColor(attValue);;
				  } else if ("imprint".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setImprinted(value);
				  } else if ("italic".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setItalic(value);
				  } else if ("outline".equals(attName)) {
				      CTOnOff onOff = CTOnOff.Factory.newInstance();
				      onOff.setVal(STOnOff.Enum.forString(attValue));
				      run.getCTR().getRPr().setOutline(onOff);
				  } else if ("position".equals(attName)) {
					  int val = Integer.parseInt(attValue);
				      run.setTextPosition(val);
				  } else if ("shadow".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setShadow(value);
				  } else if ("small-caps".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setSmallCaps(value);
				  } else if ("strikethrough".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setStrikeThrough(value);
				  } else if ("underline".equals(attName)) {
				      UnderlinePatterns value;
					try {
						value = UnderlinePatterns.valueOf(attValue.toUpperCase());
					    run.setUnderline(value);
					} catch (Exception e) {
						log.error("- [ERROR] Unrecognized underline value \"" + attValue + "\"");
					}
				  } else if ("underline-color".equals(attName)) {
					  run.setUnderlineColor(attValue);
				  } else if ("underline-theme-color".equals(attName)) {
					  run.setUnderlineThemeColor(attValue);
				  } else if ("vanish".equals(attName)) {
				      boolean value = Boolean.parseBoolean(attValue);
				      run.setVanish(value);
				  } else if ("vertical-alignment".equals(attName)) {
				      run.setVerticalAlignment(attValue);
				  }
				
			} while(cursor.toNextAttribute());
		}
		
	}

	
	/**
	 * Make a literal tabl in the run.
	 * @param run
	 * @param cursor
	 */
	private void makeTab(XWPFRun run, XmlCursor cursor) {
		run.addTab();		
	}

	/**
	 * Make a symbol within a run
	 * @param run
	 * @param cursor
	 */
	private void makeSymbol(XWPFRun run, XmlCursor cursor) {
		throw new NotImplementedException("symbol within run not implemented");
	}

	/**
	 * Construct a footnote
	 * @param para the paragraph containing the footnote.
	 * @param cursor Pointing at the &lt;fn> element
	 */
	private void makeFootnote(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException {
		
		String type = cursor.getAttributeText(DocxConstants.QNAME_TYPE_ATT);
		
		XWPFAbstractFootnoteEndnote note = null;
		if ("endnote".equals(type)) {
			note = para.getDocument().createEndnote();
		} else {
			note = para.getDocument().createFootnote();
		}
		
		// NOTE: The paragraph is not created with any initial paragraph.		
		if (cursor.toFirstChild()) {
			do {
				String tagName = cursor.getName().getLocalPart();
				String namespace = cursor.getName().getNamespaceURI();
				if ("p".equals(tagName)) {
					XWPFParagraph p = note.createParagraph();					
					makeParagraph(p, cursor, "FootnoteText");
				} else if ("table".equals(tagName)) {
					XWPFTable table = note.createTable();
					makeTable(table, cursor.getObject());
				} else {
					// There are other body-level things that could go in a footnote but 
					// we aren't worrying about them for now.
					log.warn("makeFootnote(): Unexpected element {" + namespace + "}:" + tagName + "' in <fn>. Ignored.");
				}
			} while (cursor.toNextSibling());
		}
		
		para.addFootnoteReference(note);
	
	}

	/**
	 * Gets the current ID (i.e., the last one generated).
	 * @return Current value of ID counter as a BigInteger.
	 */
	@SuppressWarnings("unused")
	private BigInteger currentId() {
		return new BigInteger(Integer.toString(idCtr));
	}

	/**
	 * Get the next ID for use in result objects.
	 * @return Next ID value as a BitInteger
	 */
	private BigInteger nextId() {
		BigInteger id = new BigInteger(Integer.toString(idCtr++));
		return id;
	}

	/**
	 * Make a break within a run.
	 * @param run Run to add the break to
	 * @param cursor Cursor pointing to the &lt;break> element
	 */
	private void makeBreak(XWPFRun run, XmlCursor cursor) throws DocxGenerationException {
		
		String typeValue = cursor.getAttributeText(DocxConstants.QNAME_TYPE_ATT);
		BreakType type = BreakType.TEXT_WRAPPING;
		if ("line".equals(typeValue) || "textWrapping".equals(typeValue)) {
			// Already set to this
		} else if ("page".equals(typeValue)) {
			type = BreakType.PAGE;
		} else if ("column".equals(typeValue)) {
			type = BreakType.COLUMN;
		} else {
			log.warn("makeBreak(): Unexpected @type value '" + typeValue + "'. Using 'line'.");			
		}
		run.addBreak(type);
		// Now move the cursor past the end of the break element
		while (cursor.currentTokenType() != TokenType.END) {
			cursor.toNextToken();
		}
		// At this point, current token is the end of the break element
	}

	/**
	 * Construct a bookmark start
	 * @param para
	 * @param cursor
	 */
	private void makeBookmarkStart(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException 
	{
		CTBookmark bookmark = para.getCTP().addNewBookmarkStart();
		bookmark.setName(cursor.getAttributeText(DocxConstants.QNAME_NAME_ATT));
		BigInteger id = nextId();
		bookmark.setId(id);
		this.bookmarkIdToIdMap.put(cursor.getAttributeText(DocxConstants.QNAME_ID_ATT), id);
	}

	/**
	 * Construct a bookmark end
	 * @param doc
	 * @param cursor
	 * @throws DocxGenerationException 
	 */
	private void makeBookmarkEnd(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException {
		CTMarkupRange bookmark = para.getCTP().addNewBookmarkEnd();
		String sourceID = cursor.getAttributeText(DocxConstants.QNAME_ID_ATT);
		BigInteger id = this.bookmarkIdToIdMap.get(sourceID);
		if (id == null) {
			throw new DocxGenerationException("No bookmark start found for bookmark end with ID '" + sourceID + "'");
		} else {
			bookmark.setId(id);
		}		
	}

	/**
	 * Construct a hyperlink
	 * @param doc
	 * @param cursor
	 */
	private void makeHyperlink(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException {
		
		String href = cursor.getAttributeText(DocxConstants.QNAME_HREF_ATT);
		
		// Hyperlink's anchor (@w:anchor) points to the name (not ID) of a bookmark.
		//
		// Alternatively, can use the @r:id attribute to point to a relationship
		// element that then points to something, normally an external resource
		// targeted by URI. 
		
		// Convention in simple WP XML is fragment identifiers are to bookmark IDs,
		// while everything else is a URI to an external resource.
		
		CTHyperlink hyperlink = para.getCTP().addNewHyperlink();
		CTR run = hyperlink.addNewR();
		run.addNewT().setStringValue(cursor.getTextValue());
		
		// Set the appropriate target:
		
		if (href.startsWith("#")) {
			// Just a fragment ID, must be to a bookmark
			String bookmarkName = href.substring(1);
			hyperlink.setAnchor(bookmarkName);
		} else {
			// Create a relationship that targets the href and use the
			// relationship's ID on the hyperlink
			// It's not yet clear from the POI API how to create a new relationship for
			// use by an external hyperlink.
			// throw new NotImplementedException("Links to external resources not yet implemented.");
		}
		
		XWPFHyperlinkRun hyperlinkRun = new XWPFHyperlinkRun(hyperlink, run, para);
		para.addRun(hyperlinkRun);
		
	}

	/**
	 * Construct an image reference
	 * @param doc
	 * @param cursor
	 */
	private void makeImage(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException {
		cursor.push();
		
		String imgUrl = cursor.getAttributeText(DocxConstants.QNAME_SRC_ATT);
		if (null == imgUrl) {
			log.error("- [ERROR] No @src attribute for image.");
			return;
		}
		URL url;
		try {
			if (!imgUrl.matches("^\\w+:.+")) {
				String baseUrl = inFile.getParentFile().toURI().toURL().toExternalForm();
				imgUrl = baseUrl + imgUrl;
			}
			url = new URL(imgUrl);
		} catch (MalformedURLException e) {
			log.error("- [ERROR] " + e.getClass().getSimpleName() + " on img/@src value: " + e.getMessage());
			return;
		}
		File imgFile = null;
		try {
			imgFile = new File(url.toURI());
		} catch (URISyntaxException e) {
			// Should never get here.
		}
		
		String imgFilename = imgFile.getName();
		String imgExtension = FilenameUtils.getExtension(imgFilename).toLowerCase();
		int width = 200; // Default width in pixels
		int height = 200; // Default height in pixels
		
		int format = getImageFormat(imgExtension);
		
        if (format == 0) {
        	// FIXME: Might be more appropriate to throw an exception here.
            log.error("Unsupported picture: " + imgFilename +
                    ". Expected emf|wmf|pict|jpeg|jpg|png|dib|gif|tiff|eps|bmp|wpg");
            cursor.pop();
            return;
        }

		BufferedImage img = null;
		int intrinsicWidth = 0;
		int intrinsicHeight = 0;
		try {		
			// FIXME: Need to limit this to the formats Java2D can read.
		    img = ImageIO.read(imgFile);
		    intrinsicWidth = img.getWidth();
		    intrinsicHeight = img.getHeight();
		} catch (IOException e) {
			log.warn("" + e.getClass().getSimpleName() + " exception loading image file '" + imgFile +"': " +
                     e.getMessage());
		}		
		 
		String widthVal = cursor.getAttributeText(DocxConstants.QNAME_WIDTH_ATT);
		if (null != widthVal) {
			try {
				width = (int) Measurement.toPixels(widthVal, getDotsPerInch());
			} catch (MeasurementException e) {
				log.error(e.getClass().getSimpleName() + ": " + e.getMessage());
				log.error("Using default width value " + width);
				width = intrinsicWidth > 0 ? intrinsicWidth : width;
			}
		} else {
			width = intrinsicWidth > 0 ? intrinsicWidth : width;			
		}

		String heightVal = cursor.getAttributeText(DocxConstants.QNAME_HEIGHT_ATT);
		if (null != heightVal) {
			try {
				height = (int) Measurement.toPixels(heightVal, getDotsPerInch());
			} catch (MeasurementException e) {
				log.error(e.getClass().getSimpleName() + ": " + e.getMessage());
				log.error("Using default height value " + height);
				height = intrinsicHeight > 0 ? intrinsicHeight : height;
			}
		} else {
			height = intrinsicHeight > 0 ? intrinsicHeight : height;
		}
		
		// At this point, the measurement is pixels. If the original specification
		// was also pixels, we need to convert to inches and then back to pixels
		// in order to apply the dots-per-inch value.
		
		// Word uses a DPI of 72, so if the current dotsPerInch is not 72, we need to
		// adjust the width and height by the difference.
		
		if (getDotsPerInch() != 72) {
			double factor = 72.0 / getDotsPerInch();
			if (widthVal != null && widthVal.matches("[0-9]+(px)?")) {
				width =  (int)Math.round(width * factor);
			}
			if (heightVal != null && heightVal.matches("[0-9]+(px)?")) {
				height = (int)Math.round(height * factor);
			}
		}				
		
		XWPFRun run = para.createRun();			

        try {
			run.addPicture(new FileInputStream(imgFile), 
					       format, 
					       imgFilename, 
					       Units.toEMU(width), 
					       Units.toEMU(height));
		} catch (Exception e) {
			log.warn("" + e.getClass().getSimpleName() + " exception adding picture for reference '" + imgFile +"': " +
 		                       e.getMessage());
		}
		cursor.pop();
	}

	/**
	 * Get the current dots-per-inch setting
	 * @return Dots (pixels) per inch
	 */
	public int getDotsPerInch() {
		return this.dotsPerInch;
	}
	
	/**
	 * Set the dots-per-inch to use when converting from pixels to absolute measurements.
	 * <p>Typical values are 72 and 96</p>
	 * @param dotsPerInch The dots-per-inch value.
	 */
	public void setDotsPerInch(int dotsPerInch) {
		this.dotsPerInch = dotsPerInch;
	}

	/**
	 * Construct an embedded object.
	 * @param para 
	 * @param cursor Cursor pointing to an <object> element.
	 */
	private void makeObject(XWPFParagraph para, XmlCursor cursor) throws DocxGenerationException {
		throw new NotImplementedException("Object handling not implemented");
		// 		cursor.push();
		//		cursor.pop();
		
	}

	/**
	 * Construct an embedded object.
	 * @param doc
	 * @param cursor Cursor pointing to an <object> element.
	 */
	private void makeObject(XWPFDocument doc, XmlCursor cursor) throws DocxGenerationException {
		throw new NotImplementedException("Object handling not implemented");
		// 		cursor.push();
		//		cursor.pop();
		
	}

	/**
	 * Construct a table.
	 * @param table Table object to construct
	 * @param xml The &lt;table&gt; element
	 * @throws DocxGenerationException
	 */
	private void makeTable(XWPFTable table, XmlObject xml) throws DocxGenerationException {
		
		// If the column widths are absolute measurements they can be set on the grid,
		// but if they are proportional, then they have to be set on at least the first
		// row's cells. The table grid is not required (it always reflects the calculated
		// width of the columns, possibly determined by applying percentage table and 
		// column widths.
		XmlCursor cursor = xml.newCursor();
		
		String widthValue = cursor.getAttributeText(DocxConstants.QNAME_WIDTH_ATT);
		if (null != widthValue) {
			try {
				table.setWidth(widthValue);
			} catch (Exception e) {
				log.warn("makeTable(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
			}
		}

		// Not setting a grid on the tables because it only uses absolute 
		// measurements. 
		
		// So setting widths on columns, which allows percentages as well as
		// explicit values.
		TableColumnDefinitions colDefs = new TableColumnDefinitions();
		cursor.toChild(DocxConstants.QNAME_COLS_ELEM);
		if (cursor.toFirstChild()) {
			do {
				TableColumnDefinition colDef = colDefs.newColumnDef();
				
				// FIXME: Set other column properties, such as row and column separators
				
				String width = cursor.getAttributeText(DocxConstants.QNAME_COLWIDTH_ATT);
				if (null != width) {
					try {
						colDef.setWidth(width, getDotsPerInch());
					} catch (MeasurementException e) {
						log.warn("makeTable(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
					}					
				} else {
					colDef.setWidthAuto();
				}
			} while (cursor.toNextSibling());
		}
		
		// populate the rows and cells.
		cursor = xml.newCursor();
		
		// Header rows:
		cursor.push();
		if (cursor.toChild(DocxConstants.QNAME_THEAD_ELEM)) {
			if (cursor.toFirstChild()) {
				RowSpanManager rowSpanManager = new RowSpanManager();
				do {
					// Process the rows
					XWPFTableRow row = makeTableRow(table, cursor.getObject(), colDefs, rowSpanManager);
					row.setRepeatHeader(true);
				} while(cursor.toNextSibling());
			}
		}
		
		// Body rows:
		
		cursor = xml.newCursor();
		if (cursor.toChild(DocxConstants.QNAME_TBODY_ELEM)) {
			if (cursor.toFirstChild()) {
				RowSpanManager rowSpanManager = new RowSpanManager();
				do {
					// Process the rows
					XWPFTableRow row = makeTableRow(table, cursor.getObject(), colDefs, rowSpanManager);
					// Adjust row as needed.
					row.getCtRow(); // For setting low-level properties.
				} while(cursor.toNextSibling());
			}
		}
		table.removeRow(0); // Remove the first row that's always added automatically (FIXME: This may not be needed any more)
	}
	
	/**
	 * Construct a table row
	 * @param table The table to add the row to
	 * @param xml The <row> element to add to the table
	 * @param colDefs Column definitions
	 * @param rowSpanManager Manages setting vertical spanning across multiple rows.
	 * @return Constructed row object
	 * @throws DocxGenerationException 
	 */
	private XWPFTableRow makeTableRow(
			XWPFTable table, 
			XmlObject xml, 
			TableColumnDefinitions colDefs, 
			RowSpanManager rowSpanManager) 
					throws DocxGenerationException {
		XmlCursor cursor = xml.newCursor();
		XWPFTableRow row = table.createRow();
		
		// FIXME: Handle attributes on rows (rowsep, colsep, etc.)
		
		cursor.push();
		cursor.toChild(DocxConstants.QNAME_TD_ELEM);
		int cellCtr = 0;
		
		do {
			// log.debug("makeTableRow(): Cell " + cellCtr);
			TableColumnDefinition colDef = colDefs.get(cellCtr);
			// Rows always have at least one cell
			// FIXME: At some point the POI API will remove the automatic creation
			// of the first cell in a row.
			XWPFTableCell cell = cellCtr == 0 ? row.getCell(0) : row.addNewTableCell();
			
			CTTcPr ctTcPr = cell.getCTTc().addNewTcPr();
			String align = cursor.getAttributeText(DocxConstants.QNAME_ALIGN_ATT);
			String valign = cursor.getAttributeText(DocxConstants.QNAME_VALIGN_ATT);
			String colspan = cursor.getAttributeText(DocxConstants.QNAME_COLSPAN_ATT);
			String rowspan = cursor.getAttributeText(DocxConstants.QNAME_ROWSPAN_ATT);
			
			
			try {
				String widthValue = cursor.getAttributeText(DocxConstants.QNAME_WIDTH_ATT);
				if (null != widthValue) {
					cell.setWidth(TableColumnDefinition.interpretWidthSpecification(widthValue, getDotsPerInch()));
				} else {
					cell.setWidth(colDef.getWidth());
					//log.debug("makeTableRow():   Setting width from column definition: " + colDef.getWidth() + " (" + colDef.getSpecifiedWidth() + ")");
				}
			} catch (Exception e) {
				log.error(e.getClass().getSimpleName() + " setting width for column " + (cellCtr + 1) + ": " + e.getMessage(), e);
			}
			if (null != valign) {
				XWPFVertAlign vertAlign = XWPFVertAlign.valueOf(valign.toUpperCase());
				cell.setVerticalAlignment(vertAlign);
			}
			if (null != colspan) {
				try {
					int spanval = Integer.parseInt(colspan);
					CTDecimalNumber spanNumber = CTDecimalNumber.Factory.newInstance();
					spanNumber.setVal(BigInteger.valueOf(spanval));
					ctTcPr.setGridSpan(spanNumber);
				} catch (NumberFormatException e) {
					log.warn("Non-numeric value for @colspan: \"" + colspan + "\". Ignored.");
				}
			}
			if (null != rowspan) {
				try {
					int spanval = Integer.parseInt(rowspan);
					CTDecimalNumber spanNumber = CTDecimalNumber.Factory.newInstance();
					spanNumber.setVal(BigInteger.valueOf(spanval));
					rowSpanManager.addColumn(cellCtr, spanval);
					CTVMerge vMerge = CTVMerge.Factory.newInstance();
					vMerge.setVal(STMerge.RESTART);
					ctTcPr.setVMerge(vMerge);
				} catch (NumberFormatException e) {
					log.warn("Non-numeric value for @rowspan: \"" + rowspan + "\". Ignored.");
				}
			}
			
			cursor.push();
			// The first cell of a span will already have a vertical span set for it.
			if (rowspan == null && cursor.toChild(DocxConstants.QNAME_VSPAN_ELEM)) {
				int spansRemaining = rowSpanManager.includeCell(cellCtr);
				if (spansRemaining < 0) {
					log.warn("Found <vspan> when there should not have been one. Ignored.");
				} else {
					ctTcPr.setVMerge(CTVMerge.Factory.newInstance());
				}
			} else {
				if (cursor.toChild(DocxConstants.QNAME_P_ELEM)) {
					do {
						XWPFParagraph p = cell.addParagraph();
						makeParagraph(p, cursor, "row");
						if (null != align) {
							ParagraphAlignment alignment = ParagraphAlignment.valueOf(align.toUpperCase());
							p.setAlignment(alignment);
						}
					} while(cursor.toNextSibling());
					// Cells always have at least one paragraph.
					cell.removeParagraph(0);
				}
			}
			cursor.pop();
			cellCtr++;
		} while(cursor.toNextSibling());
		return row;
	}

	private void setupStyles(XWPFDocument doc, XWPFDocument templateDoc) throws DocxGenerationException {
		// Load template. For now this is hard coded but will need to be
		// parameterized
				
		// Copy the template's styles to result document:
				
		try {
			XWPFStyles newStyles = doc.createStyles();
			newStyles.setStyles(templateDoc.getStyle());
		} catch (IOException e) {
			new DocxGenerationException(e.getClass().getSimpleName() + " reading template DOCX file: " + e.getMessage(), e);
		} catch (XmlException e) {
			new DocxGenerationException(e.getClass().getSimpleName() + " Copying styles from template doc: " + e.getMessage(), e);
		}
		

	}

	/**
	 * Set up any custom styles.
	 * @param doc Word doc to set up styles for
	 */
	@SuppressWarnings("unused")
	private void setupFootnoteStyles(XWPFDocument doc) throws DocxGenerationException {
		
		// Styles for footnotes:
		
		doc.createStyles(); // Make sure we have styles
		
		CTStyle style = CTStyle.Factory.newInstance();
		style.setStyleId("FootnoteReference");
		style.setType(STStyleType.CHARACTER);
		style.addNewName().setVal("footnote reference");
		style.addNewBasedOn().setVal("DefaultParagraphFont");
		style.addNewUiPriority().setVal(new BigInteger("99"));
		style.addNewSemiHidden();
		style.addNewUnhideWhenUsed();
		style.addNewRPr().addNewVertAlign().setVal(STVerticalAlignRun.SUPERSCRIPT);

		doc.getStyles().addStyle(new XWPFStyle(style));

		style = CTStyle.Factory.newInstance();
		style.setType(STStyleType.PARAGRAPH);
		style.setStyleId("FootnoteText");
		style.addNewName().setVal("footnote text");
		style.addNewBasedOn().setVal("Normal");
		style.addNewLink().setVal("FootnoteTextChar");
		style.addNewUiPriority().setVal(new BigInteger("99"));
		style.addNewSemiHidden();
		style.addNewUnhideWhenUsed();
		CTRPr rpr = style.addNewRPr();
		rpr.addNewSz().setVal(new BigInteger("20"));
		rpr.addNewSzCs().setVal(new BigInteger("20"));

		doc.getStyles().addStyle(new XWPFStyle(style));

		style  = CTStyle.Factory.newInstance();
		style.setCustomStyle(STOnOffImpl.X_1);
		style.setStyleId("FootnoteTextChar");
		style.setType(STStyleType.CHARACTER);
		style.addNewName().setVal("Footnote Text Char");
		style.addNewBasedOn().setVal("DefaultParagraphFont");
		style.addNewLink().setVal("FootnoteText");
		style.addNewUiPriority().setVal(new BigInteger("99"));
		style.addNewSemiHidden();
		rpr = style.addNewRPr();
		rpr.addNewSz().setVal(new BigInteger("20"));
		rpr.addNewSzCs().setVal(new BigInteger("20"));

		doc.getStyles().addStyle(new XWPFStyle(style));
		
	}

	/**
	 * Get the Word-specific format value. 
	 * @param imgExtension
	 * @return The format or 0 (zero) if the format is not recognized.
	 */
	private int getImageFormat(String imgExtension) {
		int format = 0;
		
		if ("emf".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_EMF;
        else if ("wmf".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_WMF;
        else if ("pict".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_PICT;
        else if ("jpeg".equals(imgExtension) || 
        		 "jpg".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_JPEG;
        else if ("png".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_PNG;
        else if ("dib".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_DIB;
        else if ("gif".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_GIF;
        else if ("tiff".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_TIFF;
        else if ("eps".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_EPS;
        else if ("bmp".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_BMP;
        else if ("wpg".equals(imgExtension)) format = XWPFDocument.PICTURE_TYPE_WPG;

		return format;
	}


}
