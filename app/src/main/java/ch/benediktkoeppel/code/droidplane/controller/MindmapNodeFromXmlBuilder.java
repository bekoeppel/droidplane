package ch.benediktkoeppel.code.droidplane.controller;

public class MindmapNodeFromXmlBuilder {



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
