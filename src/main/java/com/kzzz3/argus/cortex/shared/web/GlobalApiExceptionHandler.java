package com.kzzz3.argus.cortex.shared.web;

import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.auth.domain.RegistrationConflictException;
import com.kzzz3.argus.cortex.conversation.domain.ConversationNotFoundException;
import com.kzzz3.argus.cortex.conversation.domain.MessageNotFoundException;
import com.kzzz3.argus.cortex.friend.domain.FriendAlreadyExistsException;
import com.kzzz3.argus.cortex.friend.domain.FriendTargetNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

	@ExceptionHandler(RegistrationConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleRegistrationConflict(RegistrationConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiErrorResponse("ACCOUNT_EXISTS", exception.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiErrorResponse("INVALID_CREDENTIALS", exception.getMessage()));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
	public ResponseEntity<ApiErrorResponse> handleValidation(Exception exception) {
		String message = "Validation failed";
		if (exception instanceof MethodArgumentNotValidException notValidException) {
			FieldError fieldError = notValidException.getBindingResult().getFieldError();
			if (fieldError != null && fieldError.getDefaultMessage() != null) {
				message = fieldError.getDefaultMessage();
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(ConversationNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleConversationNotFound(ConversationNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse("CONVERSATION_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(MessageNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleMessageNotFound(MessageNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse("MESSAGE_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(FriendAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleFriendAlreadyExists(FriendAlreadyExistsException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiErrorResponse("FRIEND_ALREADY_EXISTS", exception.getMessage()));
	}

	@ExceptionHandler(FriendTargetNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleFriendTargetNotFound(FriendTargetNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse("FRIEND_TARGET_NOT_FOUND", exception.getMessage()));
	}
}
