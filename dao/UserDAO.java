package weibo.dao;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午12:57
 * To change this template use File | Settings | File Templates.
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import weibo.model.User;

public class UserDAO extends DAOSupport{
    private User user;
    public UserDAO(User user) throws Exception
    {
        super();
        this.user = user;
    }
    public User getUser()
    {
        return user;
    }
    public static int getMaxId()throws Exception
    {
        String sql = "select max(id) from t_users";
        PreparedStatement pstmt = getConnection().prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next())
        {
            return rs.getInt("max(id)");
        }
        rs.close();
        pstmt.close();
        throw new Exception("不存在user");
    }
    public void getUserbyId(int id)throws Exception
    {
        String sql = "select * from t_users where id=?";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();
        if(rs.next())
        {
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            user.setHightime(rs.getString("hightime"));
            user.setKeyword(rs.getString("keyword"));
            user.setCopywho(rs.getString("copywho"));
        }
        rs.close();
        pstmt.close();
    }
}
