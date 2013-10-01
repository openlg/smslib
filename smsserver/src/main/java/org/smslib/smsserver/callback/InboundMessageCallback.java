
package org.smslib.smsserver.callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.callback.IInboundMessageCallback;
import org.smslib.callback.events.InboundMessageEvent;
import org.smslib.helper.Common;
import org.smslib.smsserver.SMSServer;

public class InboundMessageCallback implements IInboundMessageCallback
{
	static Logger logger = LoggerFactory.getLogger(InboundMessageCallback.class);

	Connection db = null;

	@Override
	public boolean process(InboundMessageEvent event)
	{
		try
		{
			if (this.db == null) this.db = SMSServer.getInstance().getDbConnection();
			PreparedStatement s = this.db.prepareStatement("insert into smslib_in (originator, encoding, text, message_date, receive_date, gateway_id) values (?, ?, ?, ?, ?, ?)");
			s.setString(1, event.getMessage().getOriginator().getNumber());
			s.setString(2, event.getMessage().getEncoding().toShortString());
			switch (event.getMessage().getEncoding())
			{
				case Enc7:
				case EncUcs2:
					s.setString(3, event.getMessage().getPayload().getText());
					break;
				case Enc8:
					s.setString(3, Common.bytesToString(event.getMessage().getPayload().getBytes()));
					break;
			}
			s.setTimestamp(4, new Timestamp(event.getMessage().getSentDate().getTime()));
			s.setTimestamp(5, new Timestamp(event.getMessage().getCreationDate().getTime()));
			s.setString(6, event.getMessage().getGatewayId());
			s.executeUpdate();
			s.close();
			this.db.commit();
			return true;
		}
		catch (Exception e)
		{
			logger.error("Error!", e);
			if (this.db != null)
			{
				try
				{
					this.db.close();
				}
				catch (SQLException e1)
				{
					//Shallow exception on purpose...
				}
			}
			this.db = null;
			return false;
		}
	}
}
