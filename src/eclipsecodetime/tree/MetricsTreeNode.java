package eclipsecodetime.tree;

public class MetricsTreeNode {

	private String label;
	private String iconName;
	private String id;
	private Object data;
	private boolean isSeparator;
	
	public MetricsTreeNode(String label, String id) {
		this.label = label;
		this.id = id;
	}
	
	public MetricsTreeNode(String label, String id, String iconName) {
		this.label = label;
		this.iconName = iconName;
		this.id = id;
	}
	
	public MetricsTreeNode(String label, String id, String iconName, Object data) {
		this.label = label;
		this.iconName = iconName;
		this.data = data;
		this.id = id;
	}
	
	public boolean isSeparator() {
		return isSeparator;
	}

	public void setSeparator(boolean isSeparator) {
		this.isSeparator = isSeparator;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getIconName() {
		return iconName;
	}
	
	public void setIconName(String iconName) {
		this.iconName = iconName;
	}
	
	public String getId() {
		return id;
	}

	public Object getData() {
		return data;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
	
	
}
