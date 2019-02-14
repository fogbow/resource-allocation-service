package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;

public class OpenNebulaGenericRequestResponse extends GenericRequestResponse {
   private String message;
   private String errorMessage;
   private int intMessage;
   private boolean isError;

   public OpenNebulaGenericRequestResponse(String message) {
      // NOTE(pauloewerton): either 'message' or 'errorMessage' should be passed as the response content.
      super(message);
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }

   public int getIntMessage() {
      return intMessage;
   }

   public void setIntMessage(int intMessage) {
      this.intMessage = intMessage;
   }

   public boolean isError() {
      return isError;
   }

   public void setError(boolean error) {
      isError = error;
   }
}
