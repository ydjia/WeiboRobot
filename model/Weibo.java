package weibo.model;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:13
 * To change this template use File | Settings | File Templates.
 */
public class Weibo {
    private int id;
    private String mid="0";
    private String pid="";
    private String text="";
    private String time="";
    private String img_link="";

    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public String getText()
    {
        return text;
    }
        public void setText(String name)
    {
        this.text = name;
    }
    public String getMid()
    {
        return mid;
    }
    public void setMid(String mid)
    {
        this.mid = mid;
    }
    public String getPid()
    {
        return pid;
    }
    public void setPid(String pid)
    {
        this.pid = pid;
    }
    public String getTime()
    {
        return time;
    }
    public void setTime(String time)
    {
        this.time = time;
    }
    public String getImg_link()
    {
        return img_link;
    }
    public void setImg_link(String img_link)
    {
        this.img_link = img_link;
    }

}
