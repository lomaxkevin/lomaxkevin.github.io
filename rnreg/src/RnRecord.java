/* $CwuRes: src/regi/www/webapps/rnreg/src/RnRecord.java,v 1.3 2019/06/26 23:15:27 resnet Exp $
 *
 */

public class RnRecord {

    private String hostname;
    private String ha;
    private String createDate;
    private String opSystem;

    // constructor
    public RnRecord(String hostname, String ha, String createDate, String opSystem) {

        this.hostname = hostname;
        this.ha = ha;
        this.createDate = createDate;
        this.opSystem = opSystem;
    }
    
    public String getOpSystem() {
        return opSystem;
    }
    
    public String getCreateDate() {
        return createDate;
    }

    public String getHostname() {

        return hostname;

    }

    public String getHa() {

        return ha;

    }

    public void setHa(String ha) {

        this.ha = ha;

    }

}
