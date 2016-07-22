package moonstone.servlet2;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import moonstone.annotation.Annotation;
import moonstone.annotation.MissingAnnotation;
import moonstone.rule.Rule;
import moonstone.semantic.Interpretation;
import tsl.expression.term.relation.RelationSentence;
import tsl.utilities.HUtils;
import tsl.utilities.StrUtils;
import tsl.utilities.VUtils;

public class Tables {
	private MoonstoneServlet2 moonstoneServlet = null;
	private Hashtable<String, String> propertyHash = null;

	private static Vector<String> IgnorableProperties = VUtils
			.arrayToVector(new String[] { "macro", "concept", "cui", "type", "ruleid" });
	private static String TooltipNewline = "&#013;";
	private static String TableCellNewline = "<br/>";
	private static String HTMLSpace = "&nbsp;";

	public Tables(MoonstoneServlet2 mservlt) {
		this.moonstoneServlet = mservlt;
	}

	public String extractAnnotationsIntoHTMLTable(Vector<Annotation> annotations, boolean conceptsOnly, boolean cuiOnly,
			boolean allLevels) {
		this.propertyHash = new Hashtable();
		StringBuffer sb = new StringBuffer();
		if (conceptsOnly || cuiOnly) {
			annotations = this.getLowestConceptAnnotations(annotations, cuiOnly);
		}
		sb.append("<table>");

		sb.append("<TR><TH><H3><BR>MOONSTONE RESULTS</H3></TH></TR>");

		sb.append("<th>");
		sb.append("<tr>");
		sb.append("<td>SNIPPET</td>");
		sb.append("<td>OFFSET</td>");
		sb.append("<td>CONCEPT</td>");
		sb.append("<td>CUI</td>");
		sb.append("<td>TYPE</td>");
		sb.append("<td>RULE</td>");
		sb.append("<td>PROPERTIES</td>");
		sb.append("<td>RELATIONS</td>");
		sb.append("</th>");
		for (Annotation annotation : annotations) {
			if (annotation.getGoodness() > 0.25) {
				this.gatherNestedAnnotations(sb, annotation, 0, (conceptsOnly || cuiOnly), allLevels);
			}
		}
		sb.append("</table>");
		return sb.toString();
	}

	public void extractAnnotationIntoHTMLRow(StringBuffer sb, Annotation annotation, int level) {
		boolean iseven = (level % 2 == 0);
		String color = "XFFFFFF";

		switch (level) {
		case 0:
			color = "XFFFFFF";
			break;
		case 1:
			color = "XAAAAAA";
			break;
		case 2:
			color = "XFFFFAA";
			break;
		case 3:
			color = "XBBBBAA";
			break;
		default:
			color = "888888";
			break;

		}

		color = (iseven ? "XFFFFFF" : "XAAAAAA");

		String blankstr = "";
		for (int i = 0; i < level; i++) {
			blankstr += HTMLSpace + HTMLSpace;
		}
		String sent = annotation.getSentenceAnnotation().getSentence().getText();
		blankstr += "[" + (level + 1) + "]";
		String text = StrUtils.textToHtml(annotation.getText());
		text = "\"" + text + "\"";
		String concept = "*";
		String cui = "*";
		String type = "*";
		String ruleid = "*";
		String rulesexpstr = "*";
		String properties = "*";
		String relations = "*";
		String offsetstr = annotation.getStartEndString();

		if (annotation.getConcept() != null) {
			concept = annotation.getConcept().toString();
		}
		if (annotation.getCui() != null) {
			cui = annotation.getCui().toString();
		}
		if (annotation.getType() != null) {
			type = annotation.getType().toString().toUpperCase();
		}
		if (annotation.getRule() != null) {
			ruleid = annotation.getRule().getRuleID();
			rulesexpstr = this.getGrammarRuleDescriptor(ruleid);
		}
		if (annotation.getProperties() != null) {
			properties = this.getCompositePropertyAssignmentString(annotation);
		}
		if (annotation.isInterpreted() && annotation.getSemanticInterpretation().getRelationSentences(true) != null) {
			relations = this.getCompositeRelationString(annotation);
		}

		String astr = "'" + ruleid + "'";
		String bstr = "<button onclick=\"displayUserComment(" + astr + ")\">*</button>";

		sb.append("<tr BGCOLOR=\"" + color + "\">");
		sb.append("<td title=\"" + sent + "\">" + blankstr + text + "</td>");
		sb.append("<td>" + offsetstr + "</td>");
		sb.append("<td>" + concept + "</td>");
		sb.append("<td>" + cui + "</td>");
		sb.append("<td>" + type + "</td>");
		sb.append("<td title=\"" + rulesexpstr + "\">" + ruleid + "</td>");
		sb.append("<td>" + properties + "</td>");
		sb.append("<td>" + relations + "</td>");
		sb.append("</tr>");
	}

	private void gatherNestedAnnotations(StringBuffer sb, Annotation annotation, int level, boolean conceptsOnly,
			boolean allLevels) {
		if (!annotation.isInterpreted() || annotation instanceof MissingAnnotation) {
			return;
		}
		if (annotation.getRule() != null || annotation.isInterpreted()) {
			extractAnnotationIntoHTMLRow(sb, annotation, level);
			if (!conceptsOnly && allLevels && annotation.getSourceAnnotations() != null) {
				for (Annotation child : annotation.getSourceAnnotations()) {
					gatherNestedAnnotations(sb, child, level + 1, conceptsOnly, allLevels);
				}
			}
		}
	}

	private String getGrammarRuleDescriptor(String ruleid) {
		Rule rule = this.moonstoneServlet.moonstone.getControl().getSentenceGrammar().getRuleByID(ruleid);
		String nlstr = rule.getSexp().toNewlinedString();
		nlstr = StrUtils.textToHtml(nlstr);
		nlstr = nlstr.replace("<br>", TooltipNewline);
		nlstr = nlstr.replace("&quot;", "&#39;");

		nlstr = "Grammar Rule:" + TooltipNewline + nlstr;
		return nlstr;
	}

	private void gatherNestedProperties(Annotation annotation) {
		if (annotation.getProperties() != null) {
			for (String pname : annotation.getPropertyNames()) {
				if (!IgnorableProperties.contains(pname)) {
					this.propertyHash.put(pname, pname);
				}
			}
			if (annotation.getSourceAnnotations() != null) {
				for (Annotation child : annotation.getSourceAnnotations()) {
					this.gatherNestedProperties(child);
				}
			}
		}
	}

	private String getCompositePropertyAssignmentString(Annotation annotation) {
		StringBuffer sb = new StringBuffer();
		Vector<String> pnames = annotation.getPropertyNames();
		if (annotation.getProperties() != null) {
			for (String pname : pnames) {
				if (!IgnorableProperties.contains(pname)) {
					String value = getHTMLPropertyValue(annotation, pname);
					if (value != null) {
						sb.append(pname + "=" + value + TableCellNewline);
					}
				}
			}
		}
		return sb.toString();
	}

	private String getHTMLPropertyValue(Annotation annotation, String pname) {
		Object o = annotation.getProperty(pname);
		if (o != null) {
			String value = "*";
			if (o instanceof Annotation) {
				value = ((Annotation) o).getText();
				value = "\"" + value + "\"";
			} else {
				value = o.toString();
			}
			value = StrUtils.textToHtml(value);
			return value;
		}
		return null;
	}

	private String getCompositeRelationString(Annotation annotation) {
		StringBuffer sb = new StringBuffer();
		Rule rule = annotation.getRule();
		Interpretation si = annotation.getSemanticInterpretation();
		if (si != null && si.getRelationSentences(true) != null) {
			for (RelationSentence rs : si.getRelationSentences(true)) {
				sb.append(rs.getRelation().getName() + ":" + TableCellNewline);
				for (Object o : rs.getTerms()) {
					String text = null;
					if (o instanceof Annotation) {
						Annotation a = (Annotation) o;
						if (annotation.equals(a)) {
							text = "*";
						} else {
							text = StrUtils.textToHtml(a.getText());
						}
					} else {
						text = o.toString();
					}
					sb.append(HTMLSpace + HTMLSpace + "->\"" + text + "\"" + TableCellNewline);
				}
			}
		}
		return sb.toString();
	}

	private String createCommentForm(String index) {
		String bid = "comment_" + index;
		String fstr = "<form action=\"\" method=\"post\" accept-charset=UTF-8 >" + "<input type=\"text\" name=\"" + bid
				+ "\">" + "<input type=\"submit\" value=\"*\"></form>";
		return fstr;
	}

	private Vector<Annotation> getLowestConceptAnnotations(Vector<Annotation> annotations, boolean useCUI) {
		Vector<Annotation> lowest = null;
		if (annotations != null) {
			Hashtable<Object, Vector<Annotation>> chash = new Hashtable();
			annotations = Annotation.flatten(annotations);
			for (Annotation annotation : annotations) {
				Object key = null;
				if (useCUI) {
					String cui = annotation.getCui();
					if (cui != null) {
						key = cui.toLowerCase();
					}
				} else {
					key = annotation.getConcept();
				}
				if (key != null) {
					boolean dostore = true;
					Vector<Annotation> others = (Vector<Annotation>) chash.get(key);
					if (others != null) {
						Vector<Annotation> v = new Vector(others);
						for (Annotation other : v) {
							if (other.isAncestorOf(annotation)) {
								others.remove(other);
							} else if (other.equals(annotation) || annotation.isAncestorOf(other)) {
								dostore = false;
								break;
							}
						}
					}
					if (dostore) {
						VUtils.pushHashVector(chash, key, annotation);
					}
				}
			}
			for (Enumeration e = chash.keys(); e.hasMoreElements();) {
				Vector<Annotation> v = chash.get(e.nextElement());
				lowest = VUtils.append(lowest, v);
			}
			if (lowest != null) {
				Collections.sort(lowest, new Annotation.StartPositionSorter());
			}
		}
		return lowest;
	}

}
