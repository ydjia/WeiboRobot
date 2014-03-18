package weibo.model;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:01
 * To change this template use File | Settings | File Templates.
 */
public class User
{
    private int id;
    private String name;
    private String password;
    private String hightime;
    private String keyword;
    private String copywho;
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public String getPassword()
    {
        return password;
    }
    public void setPassword(String password)
    {
        this.password = password;
    }
    public String getHightime()
    {
        return hightime;
    }
    public void setHightime(String hightime)
    {
        this.hightime = hightime;
    }
    public String getKeyword()
    {
        return keyword;
    }
    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
    public String getCopywho()
    {
        return copywho;
    }
    public void setCopywho(String copywho)
    {
        this.copywho = copywho;
    }
}
