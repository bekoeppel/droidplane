package ch.benediktkoeppel.code.droidplane;


// TODO: maybe we should have proper Mindmap and MindmapNode classes
class MindmapNode {
	public String text;
	public String icon_name;
	public int icon_res_id;
	public boolean isExpandable;
	
	
	public MindmapNode(String text, String icon_name, int icon_res_id, boolean isExpandable) {
		this.text = text;
		this.icon_name = icon_name;
		this.icon_res_id = icon_res_id;
		this.isExpandable = isExpandable;
	}
}