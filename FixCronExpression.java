import com.iot.plc.database.DatabaseManager;
import java.sql.*;

public class FixCronExpression {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE tasks SET cron_expression=? WHERE id=1");
            pstmt.setString(1, "0 0/5 * * * ?");
            pstmt.executeUpdate();
            System.out.println("成功更新cron表达式");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}