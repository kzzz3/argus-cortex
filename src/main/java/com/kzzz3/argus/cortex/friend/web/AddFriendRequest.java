package com.kzzz3.argus.cortex.friend.web;

import jakarta.validation.constraints.NotBlank;

public record AddFriendRequest(
		@NotBlank String friendAccountId
) {
}
