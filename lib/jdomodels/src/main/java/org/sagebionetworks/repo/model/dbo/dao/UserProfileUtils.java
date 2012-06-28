package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class UserProfileUtils {
	
	public static void copyDtoToDbo(UserProfile dto, DBOUserProfile dbo, ObjectSchema schema) throws DatastoreException{
		if (dto.getOwnerId()==null) {
			dbo.setOwnerId(null);
		} else {
			dbo.setOwnerId(Long.parseLong(dto.getOwnerId()));
		}
		if (dto.getEtag()==null) {
			dbo.seteTag(null);
		} else {
			dbo.seteTag(Long.parseLong(dto.getEtag()));
		}
		NamedAnnotations properties = mapDtoFieldsToAnnotations(dto, schema);
		try {
			byte[] compressedProperties = JDOSecondaryPropertyUtils.compressAnnotations(properties);
			dbo.setProperties(compressedProperties);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static void copyDboToDto(DBOUserProfile dbo, UserProfile dto, ObjectSchema schema) throws DatastoreException {
		NamedAnnotations properties = null;
		try {
			byte[] compressedProperties = dbo.getProperties();
			properties = JDOSecondaryPropertyUtils.decompressedAnnotations(compressedProperties);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}		
		mapAnnotationsToDtoFields(properties, dto, schema);
		if (dbo.getOwnerId()==null) {
			dto.setOwnerId(null);
		} else {
			dto.setOwnerId(dbo.getOwnerId().toString());
		}
		if (dbo.geteTag()==null) {
			dto.setEtag(null);
		} else {
			dto.setEtag(""+dbo.geteTag());
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static NamedAnnotations mapDtoFieldsToAnnotations(UserProfile dto, ObjectSchema schema) {
		//ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		NamedAnnotations properties = new NamedAnnotations();
		Annotations a = properties.getPrimaryAnnotations();
		for (String propertyName : schemaProperties.keySet()) {
				try {
					Field field = UserProfile.class.getDeclaredField(propertyName);
					field.setAccessible(true);
					Map<String, List<String>> stringAnnots = a.getStringAnnotations();
					Class fieldType = field.getType();
					if (!(fieldType.equals(String.class) || fieldType.equals(AttachmentData.class))) {
						throw new RuntimeException("Unsupported field type "+fieldType);
					}
					if (fieldType.equals(AttachmentData.class))
					{
						AttachmentData attachment = (AttachmentData)field.get(dto);
						if (attachment != null)
						{
							stringAnnots.put(propertyName, Arrays.asList(new String[]{EntityFactory.createJSONStringForEntity(attachment)}));
						}
						else
							stringAnnots.put(propertyName, Arrays.asList(new String[]{""}));
					}
					else //String
						stringAnnots.put(propertyName, Arrays.asList(new String[]{(String)field.get(dto)}));
				} catch (NoSuchFieldException e) {
					// since UserProfile is generated by the schema, this should never happen
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (JSONObjectAdapterException e) {
					throw new RuntimeException(e);
				}
				
		}
		return properties;
		
	}
	
	@SuppressWarnings("rawtypes")
	public static void mapAnnotationsToDtoFields(NamedAnnotations properties, UserProfile dto, ObjectSchema schema) {
		//ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		Map<String, ObjectSchema> schemaProperties = schema.getProperties();
		Annotations a = properties.getPrimaryAnnotations();
		Map<String, List<String>> stringAnnots = a.getStringAnnotations();
		for (String propertyName : schemaProperties.keySet()) {
			try {
				Field field = UserProfile.class.getDeclaredField(propertyName);
				field.setAccessible(true);
				Class fieldType = field.getType();
				if (!(fieldType.equals(String.class) || fieldType.equals(AttachmentData.class))) {
					throw new RuntimeException("Unsupported field type "+fieldType);
				}
				List<String> values = stringAnnots.get(propertyName);
				if (values!=null && values.size()>0) {
					if (fieldType.equals(AttachmentData.class))
					{
						String json = values.get(0);
						AttachmentData data = null;
						if (json != null && json.length()>0)
							data = EntityFactory.createEntityFromJSONString(json, AttachmentData.class);
						field.set(dto, data);
					}
					else //String
						field.set(dto, values.get(0));
				}
			} catch (NoSuchFieldException e) {
				// since UserProfile is generated by the schema, this should never happen
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			}			
		}
	}

	
}
