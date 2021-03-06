/**
 * 
 */
package org.wordinator.xml2docx.generator;

import javax.xml.namespace.QName;

/**
 * Constants for names and namespaces and such.
 *
 */
@SuppressWarnings("unused")
public final class DocxConstants {
	
	public static final String OO_WPML_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
	public static final String SIMPLE_WP_NS = "urn:ns:wordinator:simplewpml";
	
	public static final QName QNAME_INSTR_ATT = new QName(OO_WPML_NS, "instr");
	public static final QName QNAME_ALIGN_ATT = new QName("", "align");
	public static final QName QNAME_BOLD_ATT = new QName("", "bold");
	public static final QName QNAME_BOTTOM_ATT = new QName("", "bottom");
	public static final QName QNAME_CALCULATEDWIDTH_ATT = new QName("", "calculatedWidth");
	public static final QName QNAME_CAPS_ATT = new QName("", "caps");
	public static final QName QNAME_COLSEP_ATT = new QName("", "colsep");
	public static final QName QNAME_COLSPAN_ATT = new QName("", "colspan");
	public static final QName QNAME_COLWIDTH_ATT = new QName("", "colwidth");
	public static final QName QNAME_DOUBLE_STRIKETHROUGH_ATT = new QName("", "double-strikethrough");
	public static final QName QNAME_EMBOSS_ATT = new QName("", "emboss");
	public static final QName QNAME_EMPHASIS_MARK_ATT = new QName("", "emphasis-mark");
	public static final QName QNAME_EXPAND_COLLAPSE_ATT = new QName("", "expand-collapse");
	public static final QName QNAME_FONT_ATT = new QName("", "font");
	public static final QName QNAME_FOOTER_ATT = new QName("", "footer");
	public static final QName QNAME_FORMAT_ATT = new QName("", "format");
	public static final QName QNAME_FRAME_ATT = new QName("", "frame");
	public static final QName QNAME_GUTTER_ATT = new QName("", "gutter");
	public static final QName QNAME_HEADER_ATT = new QName("", "header");
	public static final QName QNAME_HEIGHT_ATT = new QName("", "height");
	public static final QName QNAME_HIGHLIGHT_ATT = new QName("", "highlight");
	public static final QName QNAME_HREF_ATT = new QName("", "href");
	public static final QName QNAME_ID_ATT = new QName("", "id");
	public static final QName QNAME_IMPRINT_ATT = new QName("", "imprint");
	public static final QName QNAME_ITALIC_ATT = new QName("", "italic");
	public static final QName QNAME_LEFT_ATT = new QName("", "left");
	public static final QName QNAME_NAME_ATT = new QName("", "name");
	public static final QName QNAME_OUTLINE_ATT = new QName("", "outline");
	public static final QName QNAME_OUTLINE_LEVEL_ATT = new QName("", "outline-level");
	public static final QName QNAME_PAGE_BREAK_BEFORE_ATT = new QName("", "page-break-before");
	public static final QName QNAME_POSITION_ATT = new QName("", "position");
	public static final QName QNAME_RIGHT_ATT = new QName("", "right");
	public static final QName QNAME_ROWSEP_ATT = new QName("", "rowsep");
	public static final QName QNAME_ROWSPAN_ATT = new QName("", "rowspan");
	public static final QName QNAME_RULEWEIGHT_ATT = new QName("", "weight");	
	public static final QName QNAME_RULEWEIGHTUNITS_ATT = new QName("", "weight_units");	
	public static final QName QNAME_RULEWIDTH_ATT = new QName("", "width");	
	public static final QName QNAME_RULEWIDTHUNITS_ATT = new QName("", "width_units");
	public static final QName QNAME_SHADOW_ATT = new QName("", "shadow");
	public static final QName QNAME_SMALL_CAPS_ATT = new QName("", "small-caps");
	public static final QName QNAME_SRC_ATT = new QName("", "src");
	public static final QName QNAME_START_ATT = new QName("", "start");
	public static final QName QNAME_STRIKETHROUGH_ATT = new QName("", "strikethrough");
	public static final QName QNAME_STYLE_ATT = new QName("", "style");
	public static final QName QNAME_STYLEID_ATT = new QName("", "styleId");
	public static final QName QNAME_TAGNAME_ATT = new QName("", "tagName");
	public static final QName QNAME_TOP_ATT = new QName("", "top");
	public static final QName QNAME_TYPE_ATT = new QName("", "type");
	public static final QName QNAME_UNDERLINE_ATT = new QName("", "underline");
	public static final QName QNAME_UNDERLINE_COLOR_ATT = new QName("", "underline-color");
	public static final QName QNAME_VALIGN_ATT = new QName("", "valign");
	public static final QName QNAME_VANISH_ATT = new QName("", "vanish");
	public static final QName QNAME_VERTICAL_ALIGNMENT_ATT = new QName("", "vertical-alignment");
	public static final QName QNAME_WIDTH_ATT = new QName("", "width");
	public static final QName QNAME_XSLT_FORMAT_ATT = new QName("", "xslt-format");	 
	public static final QName QNAME_COLS_ELEM = new QName(SIMPLE_WP_NS, "cols");
	public static final QName QNAME_COL_ELEM = new QName(SIMPLE_WP_NS, "col");
	public static final QName QNAME_THEAD_ELEM = new QName(SIMPLE_WP_NS, "thead");
	public static final QName QNAME_TBODY_ELEM = new QName(SIMPLE_WP_NS, "tbody");
	public static final QName QNAME_TR_ELEM = new QName(SIMPLE_WP_NS, "tr");
	public static final QName QNAME_TD_ELEM = new QName(SIMPLE_WP_NS, "td");
	public static final QName QNAME_P_ELEM = new QName(SIMPLE_WP_NS, "p");
	public static final QName QNAME_ROW_ELEM = new QName(SIMPLE_WP_NS, "row");
	public static final QName QNAME_VSPAN_ELEM = new QName(SIMPLE_WP_NS, "vspan");
	public static final QName QNAME_WEIGHT_ATT = null;
}
