package weibo.dao;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-8-20
 * Time: 下午6:47
 * To change this template use File | Settings | File Templates.
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import weibo.model.Data;

public class DataDAO extends DAOSupport {
    private Data data;
    public DataDAO(Data data) throws Exception
    {
        super();
        this.data = data;
    }
    public Data getData(){
        return  data;
    }
    public void setData(int userid)throws Exception{
        String sql="insert into t_data values(?,?,?,?,?,?,now())on duplicate key update follows=?,fans=?,weibos=?,forwards=?,comments=?,update_time=now();";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, userid);
        pstmt.setInt(2,data.getFollows());
        pstmt.setInt(3,data.getFans());
        pstmt.setInt(4,data.getWeibos());
        pstmt.setInt(5,data.getForwards());
        pstmt.setInt(6,data.getComments());
        pstmt.setInt(7,data.getFollows());
        pstmt.setInt(8,data.getFans());
        pstmt.setInt(9,data.getWeibos());
        pstmt.setInt(10,data.getForwards());
        pstmt.setInt(11,data.getComments());
        pstmt.executeUpdate();
        pstmt.close();
    }
    public void getData(int userid)throws Exception{
        String sql="select * from t_data where id=?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, userid);
        ResultSet rs = pstmt.executeQuery();
        if(rs.next())
        {
           data.setFollows(rs.getInt("follows"));
           data.setFans(rs.getInt("fans"));
           data.setWeibos(rs.getInt("weibos"));
           data.setForwards(rs.getInt("forwards"));
           data.setComments(rs.getInt("comments"));
        }
        rs.close();
        pstmt.close();
    }
}
