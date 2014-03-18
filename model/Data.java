package weibo.model;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-8-20
 * Time: 下午6:35
 * To change this template use File | Settings | File Templates.
 */

public class Data {
    private int id;
    private int follows;
    private int fans;
    private int weibos;
    private int forwards;
    private int comments;
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public int getFollows()
    {
        return follows;
    }
    public void setFollows(int follows)
    {
        this.follows = follows;
    }
    public int getFans()
    {
        return fans;
    }
    public void setFans(int fans)
    {
        this.fans = fans;
    }
    public int getWeibos()
    {
        return weibos;
    }
    public void setWeibos(int weibos)
    {
        this.weibos = weibos;
    }
    public int getForwards()
    {
        return forwards;
    }
    public void setForwards(int forwards)
    {
        this.forwards =forwards;
    }
    public int getComments()
    {
        return comments;
    }
    public void setComments(int comments)
    {
        this.comments = comments;
    }
}
