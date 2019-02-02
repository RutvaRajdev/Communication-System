package edu.northeastern.ccs.im;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

    private Message message;

    private String noAckMessage = "NAK 2 -- 2 --";

    @BeforeEach
    public void setUp() {
        message = Message.makeMessage("BCT","ABC", "XYZ");
    }

    @Test
    public void makeQuitMessage() {
        assertEquals("BYE 3 ABC 2 --", Message.makeQuitMessage("ABC").toString());
    }

    @Test
    public void makeBroadcastMessage() {
        assertEquals("BCT 4 JOHN 3 ABC", Message.makeBroadcastMessage("JOHN","ABC").toString());
    }

    @Test
    public void makeHelloMessage() {
        assertEquals("HLO 2 -- 3 ABC", Message.makeHelloMessage("ABC").toString());
    }

    @Test
    public void makeMessage() {
        assertEquals("BYE 3 ABC 2 --", Message.makeMessage("BYE","ABC", "").toString());
        assertEquals("BCT 0  3 ABC", Message.makeMessage("BCT","", "ABC").toString());
        assertEquals("HLO 0  3 ABC", Message.makeMessage("HLO","", "ABC").toString());
        assertEquals("ACK 0  2 --", Message.makeMessage("ACK","", "ABC").toString());
        assertEquals(noAckMessage, Message.makeMessage("NAK","", "ABC").toString());
        assertEquals("REG 0  3 ABC", Message.makeMessage("REG","", "ABC").toString());
        assertEquals("DEL 0  2 --", Message.makeMessage("DEL","", "ABC").toString());
        assertEquals("UUN 0  3 ABC", Message.makeMessage("UUN","", "ABC").toString());
        assertEquals("UPW 0  3 ABC", Message.makeMessage("UPW","", "ABC").toString());
        assertEquals("PRV 0  6 ABC:Hi", Message.makeMessage("PRV","", "ABC:Hi").toString());
        assertEquals("GRP 0  7 grp1:Hi", Message.makeMessage("GRP","", "grp1:Hi").toString());

        assertEquals("GRC 5 user1 7 team103", Message.makeMessage("GRC","user1", "team103").toString());
        assertEquals("GRA 5 user1 21 team103:user11 user12", Message.makeMessage("GRA","user1", "team103:user11 user12").toString());
        assertEquals("GRD 5 user1 7 team103", Message.makeMessage("GRD","user1", "team103").toString());
        assertEquals("GRR 5 user1 21 team103:user11 user12", Message.makeMessage("GRR","user1", "team103:user11 user12").toString());
        assertEquals("GRU 5 user1 19 team103:team103-new", Message.makeMessage("GRU","user1", "team103:team103-new").toString());
        assertEquals("GRI 5 user1 21 team103:thisisanimage", Message.makeMessage("GRI","user1", "team103:thisisanimage").toString());
        assertEquals("PRI 5 user1 19 user2:thisisanimage", Message.makeMessage("PRI","user1", "user2:thisisanimage").toString());
        assertEquals("UDE 5 user1 2 --", Message.makeMessage("UDE","user1", "").toString());
        assertEquals("GDE 5 user1 6 group1", Message.makeMessage("GDE","user1", "group1").toString());
        assertEquals("SUN 5 user1 5 user2", Message.makeMessage("SUN","user1", "user2").toString());
        assertEquals("SGN 5 user1 2 g1", Message.makeMessage("SGN","user1", "g1").toString());
        assertEquals("SUT 5 user1 17 user2 08:00 10:00", Message.makeMessage("SUT","user1", "user2 08:00 10:00").toString());
        assertEquals("SGT 5 user1 17 user2 08:00 10:00", Message.makeMessage("SGT","user1", "user2 08:00 10:00").toString());
        assertEquals("PCT 5 user1 9 user2:OFF", Message.makeMessage("PCT","user1", "user2:OFF").toString());

    }

    @Test
    public void testNullPointerExceptionMakeMessage() {
        assertThrows(NullPointerException.class,
                ()-> Message.makeMessage("other", "", "ABC").toString());
    }


    @Test
    public void makeNoAcknowledgeMessage() {
        assertEquals(noAckMessage, Message.makeNoAcknowledgeMessage().toString());
    }

    @Test
    public void makeAcknowledgeMessage() {
        assertEquals("ACK 3 ABC 2 --", Message.makeAcknowledgeMessage("ABC").toString());
    }

    @Test
    public void makePrivateMessage() {
        assertEquals("PRV 3 ABC 4 B:Hi", Message.makePersonalMessage("ABC", "Hi", "B").toString());
    }

    @Test
    public void makeViewUserMessage() {
        assertEquals("UDE 3 ABC 2 --", Message.makeViewUserMessage("ABC").toString());
    }

    @Test
    public void makeViewUsersInGroupMessage() {
        assertEquals("GDE 3 ABC 6 group1", Message.makeViewUsersInGroupMessage("ABC", "group1").toString());
    }

    @Test
    public void makePersonalImageMessage() {
        assertEquals("PRI 3 ABC 4 B:Hi", Message.makePersonalImageMessage("ABC", "Hi", "B").toString());
    }

    @Test
    public void makeGroupMessage() {
        assertEquals("GRP 3 ABC 4 B:Hi", Message.makeGroupMessage("ABC", "Hi", "B").toString());
    }

    @Test
    public void makeGroupImageMessage() {
        assertEquals("GRI 3 ABC 4 B:Hi", Message.makeGroupImageMessage("ABC", "Hi", "B").toString());
    }

    @Test
    public void makeSearchByUNameMsg() {
        assertEquals("SUN 3 ABC 1 B", Message.makeSearchMsgByUNameMessage("ABC", "B").toString());
    }

    @Test
    public void makeSearchMsgByTimeStampForUserMessage() {
        assertEquals("SUT 2 VV 25 SS 12:11:2018 08:00 10:00", Message.makeSearchMsgByTimeStampForUserMessage("VV", "SS 12:11:2018 08:00 10:00").toString());
    }

    @Test
    public void makeSearchMsgByTimeStampForUserMessage2() {
        SimpleDateFormat formatter1=new SimpleDateFormat("MM/dd/yyyy HH:mm");
        formatter1.setTimeZone(TimeZone.getTimeZone("EST"));
        String startTime = "12/11/2018 08:00";
        String endTime = "12/11/2018 12:00";
        try {
            Date date1=formatter1.parse(startTime);
            Date date2 = formatter1.parse(endTime);
            assertEquals("SUT 2 VV 62 SS, Tue Dec 11 13:00:00 GMT 2018, Tue Dec 11 17:00:00 GMT 2018", Message.makeSearchMsgByTimeStampForUserMessage("VV", "SS", date1, date2).toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void makeSearchMsgByTimeStampForGroupMessage() {
        assertEquals("SGT 2 VV 30 team103 12:11:2018 08:00 10:00", Message.makeSearchMsgByTimeStampForGroupMessage("VV", "team103 12:11:2018 08:00 10:00").toString());
    }

    @Test
    public void makeSearchMsgByTimeStampForGroupMessage2() {
        SimpleDateFormat formatter1=new SimpleDateFormat("MM/dd/yyyy HH:mm");
        formatter1.setTimeZone(TimeZone.getTimeZone("EST"));
        String startTime = "12/11/2018 08:00";
        String endTime = "12/11/2018 12:00";
        try {
            Date date1=formatter1.parse(startTime);
            Date date2 = formatter1.parse(endTime);
            assertEquals("SGT 2 VV 67 team103, Tue Dec 11 13:00:00 GMT 2018, Tue Dec 11 17:00:00 GMT 2018", Message.makeSearchMsgByTimeStampForGroupMessage("VV", "team103", date1, date2).toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void makeUpdateParentalControlMsg() {
        assertEquals("PCT 2 VV 5 SS:ON", Message.makeUpdateParentalControlMsg("VV","SS:ON").toString());
    }

    @Test
    public void makeUpdateParentalControlMsg2() {
        assertEquals("PCT 2 VV 3 OFF", Message.makeUpdateParentalControlMsg("VV","", "OFF").toString());
    }


    @Test
    public void makeSearchByGNameMsg() {
        assertEquals("SGN 3 ABC 2 g1", Message.makeSearchMsgByGNameMessage("ABC", "g1").toString());
    }

    @Test
    public void makeGroupRenameMessage() {
        assertEquals("GRU 3 ABC 5 G1:G2", Message.makeGroupRenameMessage("ABC", "G2", "G1").toString());
    }

    @Test
    public void makeSimpleLoginMessage() {
        assertEquals("HLO 3 ABC 3 pwd", Message.makeSimpleLoginMessage("ABC", "pwd").toString());
    }

    @Test
    public void getName() {
        assertEquals("ABC", message.getName());
    }

    @Test
    public void getText() {
        assertEquals("XYZ", message.getText());
    }

    @Test
    public void isAcknowledge() {
        message = Message.makeAcknowledgeMessage("ABC");
        assertTrue(message.isAcknowledge());
    }

    @Test
    public void isNotAcknowledge() {
        message = Message.makeDeleteMessage("ABC");
        assertTrue(!message.isAcknowledge());
    }

    @Test
    public void isBroadcastMessage() {
        message = Message.makeBroadcastMessage("JOHN","ABC");
        assertTrue(message.isBroadcastMessage());
    }

    @Test
    public void isViewUserMessage() {
        message = Message.makeViewUserMessage("JOHN");
        assertTrue(message.isViewUserDMessage());
    }

    @Test
    public void isViewUsersInGroupMessage() {
        message = Message.makeViewUsersInGroupMessage("JOHN", "group1");
        assertTrue(message.isViewUsersInGroupMessage());
    }

    @Test
    public void isSearchByUNameMsg() {
        message = Message.makeSearchMsgByUNameMessage("JOHN", "B");
        assertTrue(message.isSearchMsgByUNameMsg());
    }

    @Test
    public void isSearchByTimeStampForUserMsg() {
        message = Message.makeSearchMsgByTimeStampForUserMessage("VV", "SS 12:11:2018 08:00");
        assertTrue(message.ismakeSearchMsgByTimeStampForUserMessage());
    }

    @Test
    public void isSearchByTimeStampForGrpMsg() {
        message = Message.makeSearchMsgByTimeStampForGroupMessage("VV", "SS 12:11:2018 08:00");
        assertTrue(message.ismakeSearchMsgByTimeStampForGroupMessage());
    }

    @Test
    public void isUpateParentalControlMsg() {
        message = Message.makeUpdateParentalControlMsg("VV", "SS:ON");
        assertTrue(message.isUpdatePCMsg());
    }

    @Test
    public void isSearchByGNameMsg() {
        message = Message.makeSearchMsgByGNameMessage("JOHN", "g1");
        assertTrue(message.isSearchMsgByGNameMsg());
    }

    @Test
    public void isPrivateMessage() {
        message = Message.makePersonalMessage("JOHN","ABC:Hi");
        assertTrue(message.isPrivateMessage());
    }

    @Test
    public void isPersonalContaingImageMessage() {
        message = Message.makePersonalImageMessage("JOHN","ABC:Hi");
        assertTrue(message.isPersonalContaingImageMessage());
    }

    @Test
    public void isGroupContainingImageMessage() {
        message = Message.makeGroupImageMessage("JOHN","ABC:Hi");
        assertTrue(message.isGroupContainingImageMessage());
    }

    @Test
    public void isGroupMessage() {
        message = Message.makeGroupMessage("JOHN","ABC:Hi");
        assertTrue(message.isGroupMessage());
    }

    @Test
    public void isCreateGroupMessage() {
        message = Message.makeGroupCreationMessage("JOHN","ABC:Hi");
        assertTrue(message.isCreateGroupMessage());
    }

    @Test
    public void isRenameGroupMessage() {
        message = Message.makeGroupRenameMessage("JOHN","G1:G2");
        assertTrue(message.isGroupRenameMessage());
    }

    @Test
    public void isDeleteGroupMessage() {
        message = Message.makeGroupDeletionMessage("JOHN","Group1");
        assertTrue(message.isDeleteGroupMessage());
    }

    @Test
    public void isAddUsersToGroupMessage() {
        message = Message.makeAddUsersToGrpMessage("JOHN","user1 user2 user3", "team103");
        assertTrue(message.isAddUsersToGroupMessage());
    }

    @Test
    public void isRemoveUsersFromGroupMessage() {
        message = Message.makeRemoveUserFromGrpMessage("JOHN","user1 user2 user3", "team103");
        assertTrue(message.isRemoveUsersFromGroupMessage());
    }

    @Test
    public void isNotBroadcastMessage() {
        message = Message.makeRegisterMessage("JOHN","ABC");
        assertTrue(!message.isBroadcastMessage());
    }

    @Test
    public void isDisplayMessage() {
        message = Message.makeBroadcastMessage("JOHN","ABC");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage2() {
        message = Message.makePersonalMessage("JOHN","ABC:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage3() {
        message = Message.makeGroupMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage4() {
        message = Message.makePersonalImageMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage5() {
        message = Message.makeGroupImageMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage6() {
        message = Message.makeSearchMsgByTimeStampForGroupMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage7() {
        message = Message.makeSearchMsgByTimeStampForUserMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage8() {
        message = Message.makeSearchMsgByUNameMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }

    @Test
    public void isDisplayMessage9() {
        message = Message.makeSearchMsgByGNameMessage("JOHN","Grp1:Hi");
        message.isDisplayMessage();
    }


    @Test
    public void isInitialization() {
        message = Message.makeHelloMessage("ABC");
        assertTrue(message.isInitialization());
    }

    @Test
    public void terminate() {
        message = Message.makeQuitMessage("ABC");
        assertTrue(message.terminate());
    }

    @Test
    public void isNotTerminateMessage() {
        message = Message.makeDeleteMessage("ABC");
        assertTrue(!message.terminate());
    }

    @Test
    public void testToString() {
        assertEquals(noAckMessage, Message.makeMessage("NAK","", "ABC").toString());
    }

    @Test
    public void isUpdatePasswordType() {
        message = Message.makeUpdatePasswordMessage("JOHN","ABC");
        assertTrue(message.isUpdatePasswordType());
    }

    @Test
    public void isUpdateUsernameType() {
        message = Message.makeUpdateUsernameMessage("JOHN","ABC");
        assertTrue(message.isUpdateUsernameType());
    }

    @Test
    public void isDeleteMessage() {
        message = Message.makeDeleteMessage("JOHN");
        assertTrue(message.isDeleteMessage());
    }

    @Test
    public void isRegistrationMessage() {
        message = Message.makeRegisterMessage("JOHN","ABC");
        assertTrue(message.isRegistrationMessage());
    }


}