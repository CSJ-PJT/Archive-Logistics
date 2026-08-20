package com.csj.archive.logistics.runtime;
import com.fasterxml.jackson.databind.*; import org.springframework.http.*; import org.springframework.stereotype.Component; import org.springframework.web.client.*; import java.time.*;
@Component public class ArchiveOsRuntimePublisher {
 private final RestClient.Builder builder; private final ArchiveOsRuntimeProperties properties; private final ObjectMapper mapper;
 public ArchiveOsRuntimePublisher(RestClient.Builder builder,ArchiveOsRuntimeProperties properties,ObjectMapper mapper){this.builder=builder;this.properties=properties;this.mapper=mapper;}
 public Result publish(JsonNode payload){
  if(!properties.isEnabled()||properties.getBaseUrl().isBlank()||properties.getToken().isBlank())return new Result(ArchiveOsDeliveryStatus.CONFIG_ERROR,"CONFIG",false);
  try { String body=builder.clone().baseUrl(properties.getBaseUrl()).build().post().uri(properties.getPath()).contentType(MediaType.APPLICATION_JSON).headers(h->{h.setBearerAuth(properties.getToken());h.set("X-Archive-Source-System","archive-logistics");h.set("X-Archive-Service-Scope","runtime:ingest");}).body(payload).retrieve().body(String.class); JsonNode data=mapper.readTree(body==null?"{}":body).path("data"); return data.path("accepted").asBoolean()||data.path("duplicate").asBoolean()?new Result(ArchiveOsDeliveryStatus.PUBLISHED,"OK",false):new Result(ArchiveOsDeliveryStatus.NON_RETRYABLE_ERROR,"CONTRACT",false);
  } catch(HttpClientErrorException.Unauthorized|HttpClientErrorException.Forbidden e){return new Result(ArchiveOsDeliveryStatus.CONFIG_ERROR,"HTTP_"+e.getStatusCode().value(),false);
  } catch(HttpClientErrorException e){return new Result(ArchiveOsDeliveryStatus.NON_RETRYABLE_ERROR,"HTTP_"+e.getStatusCode().value(),false);
  } catch(HttpServerErrorException|ResourceAccessException e){return new Result(ArchiveOsDeliveryStatus.RETRY_WAIT,"TRANSIENT",true);}
  catch(Exception e){return new Result(ArchiveOsDeliveryStatus.RETRY_WAIT,"TRANSPORT",true);}
 }
 public record Result(ArchiveOsDeliveryStatus status,String code,boolean retryable){}
}
