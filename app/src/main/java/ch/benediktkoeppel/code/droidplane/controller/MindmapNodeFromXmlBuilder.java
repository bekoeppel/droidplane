package ch.benediktkoeppel.code.droidplane.controller;

public class MindmapNodeFromXmlBuilder {


//    public static MindmapNode parse(Node node, MindmapNode parentNode, Mindmap mindmap) {
//        return null;
//
//        NodeList richtextNodeList = tmpElement.getChildNodes();
//        for (int i = 0; i < richtextNodeList.getLength(); i++) {
//            Node n = richtextNodeList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("richcontent")) {
//                Element richcontentElement = (Element)n;
//                String typeAttribute = richcontentElement.getAttribute("TYPE");
//                if (typeAttribute.equals("NODE") || typeAttribute.equals("NOTE") || typeAttribute.equals("DETAILS")) {
//
//                    // extract the whole rich text (XML), to show in a WebView activity
//                    try {
//                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
//                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
//                        transformer.transform(new DOMSource(richtextNodeList.item(0)), new StreamResult(boas));
//                        richTextContent = boas.toString();
//                    } catch (TransformerException e) {
//                        e.printStackTrace();
//                    }
//
//                    // if the node has no text itself, then convert the rich text content to a text
//                    if (text == null || text.equals("")) {
//                        // convert the content (text only) into a string, to show in the normal list view
//                        text = Html.fromHtml(richcontentElement.getTextContent()).toString();
//                    }
//                }
//            }
//        }
//
//        // extract styles
//        NodeList styleNodeList = tmpElement.getChildNodes();
//        boolean isBold = false;
//        boolean isItalic = false;
//        for (int i = 0; i < styleNodeList.getLength(); i++) {
//            Node n = styleNodeList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("font")) {
//                Element fontElement = (Element)n;
//                if (fontElement.hasAttribute("BOLD") && fontElement.getAttribute("BOLD").equals("true")) {
//                    Log.d(MainApplication.TAG, "Found bold node");
//                    isBold = true;
//                }
//                if (fontElement.hasAttribute("ITALIC") && fontElement.getAttribute("ITALIC").equals("true")) {
//                    isItalic = true;
//                }
//            }
//        }
//
//        // extract icons
//        ArrayList<String> iconNames = getIcons(node);
//
//        // find out if it has sub nodes
//        // TODO: this should just go into a getter
//        boolean isExpandable = (getNumChildMindmapNodes(node) > 0);
//
//        // extract link
//        String linkAttribute = tmpElement.getAttribute("LINK");
//        Uri link;
//        if (!linkAttribute.equals("")) {
//            link = Uri.parse(linkAttribute);
//        } else {
//            link = null;
//        }
//
//        // get cloned node's info
//        String treeIdAttribute = tmpElement.getAttribute("TREE_ID");
//
//        // get arrow link destinations
//        ArrayList<String> arrowLinkDestinationIds = new ArrayList<>();
//        NodeList arrowlinkList = tmpElement.getChildNodes();
//        for (int i = 0; i< arrowlinkList.getLength(); i++) {
//            Node n = arrowlinkList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("arrowlink")) {
//                Element arrowlinkElement = (Element)n;
//                String destinationId = arrowlinkElement.getAttribute("DESTINATION");
//                arrowLinkDestinationIds.add(destinationId);
//            }
//        }
//
//
//        MindmapNodeFromXmlBuilder.getChildNodes(mindmap, mindmapNode, node);
//
//        return MindmapNode.builder()
//                .mindmap(mindmap)
//                .parentNode(parentNode)
//                .node(node)
//                .id(id)
//                .numericId(numericId)
//                .richTextContent(richTextContent)
//                .text(text)
//                .isBold(isBold)
//                .isItalic(isItalic)
//                .iconNames(iconNames)
//                .isExpandable(isExpandable)
//                .link(link)
//                .treeIdAttribute(treeIdAttribute)
//                .arrowLinkDestinationIds(arrowLinkDestinationIds)
//                .numChildMindmapNodes(getNumChildMindmapNodes())
//                .build();
//    }
//
//
//
//    /**
//     * Generates and returns the child nodes of this MindmapNode. getChildNodes() does lazy loading, i.e. it
//     * generates the child nodes on demand and stores them in childMindmapNodes.
//     *
//     * TODO: not so sure if this is really doing lazy loading. We probably call this already pretty early on. Should check.
//     *
//     * TODO: this should be called differently, to not confuse it with getChildNodes of Node
//     *
//     * @return ArrayList of this MindmapNode's child nodes
//     */
//    public static List<MindmapNode> getChildNodes(Mindmap mindmap, MindmapNode mindmapNode, Node node) {
//
//        synchronized (mindmapNode) {
//
//            // if we haven't loaded the childMindmapNodes before
//            if (mindmapNode.getChildMindmapNodes() == null) {
//
//                // fetch all child DOM Nodes, convert them to MindmapNodes
//                List<MindmapNode> newChildMindmapNodes = new ArrayList<>();
//                NodeList childNodes = node.getChildNodes();
//                for (int i = 0; i < childNodes.getLength(); i++) {
//                    Node tmpNode = childNodes.item(i);
//
//                    if (isMindmapNode(tmpNode)) {
//                        MindmapNode newChildMindmapNode = parse(tmpNode, mindmapNode, mindmap);
//                        newChildMindmapNodes.add(newChildMindmapNode);
//                    }
//                }
//
//                mindmapNode.setChildMindmapNodes(Collections.unmodifiableList(newChildMindmapNodes));
//
//            }
//
//            // we already did that before, so return the previous result
//            else {
//                Log.d(MainApplication.TAG, "Returning cached childMindmapNodes");
//            }
//
//            return mindmapNode.getChildMindmapNodes();
//        }
//    }
//
//
//
//
//    /**
//     * Extracts the list of icons from a node and returns the names of the icons as ArrayList.
//     *
//     * @return list of names of the icons
//     */
//    private static ArrayList<String> getIcons(Node node) {
//
//        ArrayList<String> iconsNames = new ArrayList<>();
//
//        NodeList childNodes = node.getChildNodes();
//        for (int i = 0; i < childNodes.getLength(); i++) {
//
//            Node n = childNodes.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE) {
//                Element e = (Element)n;
//
//                if (e.getTagName().equals("icon") && e.hasAttribute("BUILTIN")) {
//                    iconsNames.add(e.getAttribute("BUILTIN"));
//                }
//            }
//        }
//
//        return iconsNames;
//    }
//
//
//
//    /**
//     * Returns the number of child Mindmap nodes
//     *
//     * @return
//     */
//    public static int getNumChildMindmapNodes(Node node) {
//
//        int numMindmapNodes = 0;
//
//        NodeList childNodes = node.getChildNodes();
//        for (int i = 0; i < childNodes.getLength(); i++) {
//
//            Node n = childNodes.item(i);
//            if (isMindmapNode(n)) {
//                numMindmapNodes++;
//            }
//        }
//
//        return numMindmapNodes;
//    }
}
