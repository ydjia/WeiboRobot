package weibo.dao;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:15
 * To change this template use File | Settings | File Templates.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import weibo.model.Weibo;
public class WeiboDAO extends DAOSupport {
    private Weibo  weibo;
    public WeiboDAO(Weibo weibo)throws Exception
    {
        super();
        this.weibo = weibo;
    }
    public Weibo getWeibo()
    {
        return weibo;
    }
/*    public static int getMaxId(int userid)throws Exception
    {
        String sql = "select max(id) from t_weibo?";
        PreparedStatement pstmt = getConnection().prepareStatement(sql);
        pstmt.setInt(1, userid);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next())
        {
            return rs.getInt("max(id)");
        }
        throw new Exception("不存在weibo");
    }*/
    public void getWeibobyId(int id,int userid)throws Exception
    {
        String sql = "select * from t_weibo? where id=?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, userid);
        pstmt.setInt(2, id);
        ResultSet rs = pstmt.executeQuery();
        if(rs.next())
        {
            weibo.setText(rs.getString("text"));
            weibo.setPid(rs.getString("pid"));
            weibo.setImg_link(rs.getString("img_link"));
            weibo.setTime(rs.getString("time"));
        }
        rs.close();
        pstmt.close();
    }
   public void saveWeibo(int userid)throws Exception
   {
       String sql="insert into t_weibo? (text,mid,pid,img_link,time)values(?,?,?,?,?)";
       PreparedStatement pstmt = connection.prepareStatement(sql);
       pstmt.setInt(1, userid);
       pstmt.setString(2,weibo.getText());
       pstmt.setString(3,weibo.getMid());
       pstmt.setString(4,weibo.getPid());
       pstmt.setString(5,weibo.getImg_link());
       pstmt.setString(6,weibo.getTime());
       pstmt.executeUpdate();
       pstmt.close();
   }
   public int getTotal(int userid)throws Exception
   {
       String sql="select max(id) from t_weibo?";
       PreparedStatement pstmt = connection.prepareStatement(sql);
       pstmt.setInt(1, userid);
       ResultSet rs = pstmt.executeQuery();
       if(rs.next())
       {
         return rs.getInt("max(id)");
       }
       rs.close();
       pstmt.close();
       return 0;
   }
   public String getMaxMid(int userid)throws Exception
   {
       String sql = "select * from t_weibo?";
       PreparedStatement pst = connection.prepareStatement(sql);
       pst.setLong(1,userid);
       ResultSet re = pst.executeQuery();
       if(re.next()){
           String sql1 = "select max(mid) from t_weibo?";
           PreparedStatement pst1 = connection.prepareStatement(sql1);
           pst1.setLong(1, userid);
           ResultSet rs = pst1.executeQuery();
           String max_mid = "0";
           if(rs.next()){
               max_mid = rs.getString("max(mid)");
           }
           pst1.close();
           pst.close();
           return max_mid;
       }
       else{
           pst.close();
            return "0";
       }
   }
}
