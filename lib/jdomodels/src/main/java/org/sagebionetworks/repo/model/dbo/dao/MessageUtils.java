package org.sagebionetworks.repo.model.dbo.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;

public class MessageUtils {
	
	public static List<Message> convertDBOs(List<DBOMessage> dbos) {
		List<Message> dtos = new ArrayList<Message>();
		for (DBOMessage dbo : dbos) {
			dtos.add(convertDBO(dbo));
		}
		return dtos;
	}
	
	public static Message convertDBO(DBOMessage dbo) {
		Message dto = new Message();
		dto.setMessageId(dbo.getMessageId().toString());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setRecipientType(ObjectType.valueOf(dbo.getRecipientType()));
		try {
			dto.setRecipients(unzip(dbo.getRecipients()));
		} catch (IOException e) {
			throw new DatastoreException("Could not unpack the list of intended recipients", e);
		}
		dto.setMessageFileHandleId(dbo.getFileHandleId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setSubject(dbo.getSubject());
		return dto;
	}
	
	/**
	 * Checks for all required fields of the DBO
	 */
	public static void validateDBO(DBOMessage dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Message ID must be specified");
		}
		if (dbo.getCreatedBy() == null) {
			throw new IllegalArgumentException("Sender's ID must be specified");
		}
		if (dbo.getRecipientType() == null) {
			throw new IllegalArgumentException("Recipient type must be specified");
		}
		switch (ObjectType.valueOf(dbo.getRecipientType())) {
		case PRINCIPAL:
		case ENTITY:
			break;
		default:
			throw new IllegalArgumentException("Recipient type must be either PRINCIPAL or ENTITY");
		}
		try {
			if (dbo.getRecipients() == null || unzip(dbo.getRecipients()).size() <= 0) {
				throw new IllegalArgumentException("Recipients must be specified");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (dbo.getFileHandleId() == null) {
			throw new IllegalArgumentException("Message body's file handle must be specified");
		}
		if (dbo.getCreatedOn() == null) {
			throw new IllegalArgumentException("Time of sending must be specified");
		}
	}
	
	public static DBOMessage convertDTO(Message dto) {
		DBOMessage dbo = new DBOMessage();
		dbo.setMessageId(Long.parseLong(dto.getMessageId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setRecipientType(dto.getRecipientType().name());
		try {
			dbo.setRecipients(zip(dto.getRecipients()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dbo.setFileHandleId(Long.parseLong(dto.getMessageFileHandleId()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setSubject(dto.getSubject());
		return dbo;
	}
	
	/**
	 * Gzips the given list. 
	 * @param longs Each element must be parsable as a long
	 * @return A gzipped byte array of long ints 
	 */
	protected static byte[] zip(Set<String> longs) throws IOException {
		// Convert the Strings into Longs into Bytes
		ByteBuffer converter = ByteBuffer.allocate(Long.SIZE / 8 * longs.size());
		for (String num : longs) {
			converter.putLong(KeyFactory.stringToKey(num));
		}
		
		// Zip up the bytes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipped = new GZIPOutputStream(out);
		zipped.write(converter.array());
		zipped.flush();
		zipped.close();
		return out.toByteArray();
	}
	
	/**
	 * Un-gzips the given array.  
	 * @param zippedLongs Gzipped array of long ints
	 * @return A list of longs in base 10 string form
	 */
	protected static Set<String> unzip(byte[] zippedLongs) throws IOException {
		// Unzip the bytes
		ByteArrayInputStream in = new ByteArrayInputStream(zippedLongs);
		GZIPInputStream unzip = new GZIPInputStream(in);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (unzip.available() > 0) {
			out.write(unzip.read());
		}
		
		// Convert to bytes
		ByteBuffer converter = ByteBuffer.wrap(out.toByteArray());
		LongBuffer converted = converter.asLongBuffer();
		Set<String> verbose = new HashSet<String>();
		while (converted.hasRemaining()) {
			verbose.add(Long.toString(converted.get()));
		}
		return verbose;
	}
	
	public static MessageStatus convertDBO(DBOMessageStatus dbo) {
		MessageStatus dto = new MessageStatus();
		dto.setMessageId(dbo.getMessageId().toString());
		dto.setRecipientId(dbo.getRecipientId().toString());
		dto.setStatus(MessageStatusType.valueOf(dbo.getStatus()));
		return dto;
	}
	
	public static DBOMessageStatus convertDTO(MessageStatus dto) {
		DBOMessageStatus dbo = new DBOMessageStatus();
		dbo.setMessageId(Long.parseLong(dto.getMessageId()));
		dbo.setRecipientId(Long.parseLong(dto.getRecipientId()));
		dbo.setStatus(dto.getStatus());
		return dbo;
	}
}
