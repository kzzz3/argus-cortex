package com.kzzz3.argus.cortex.friend.web;

import com.kzzz3.argus.cortex.friend.application.AddFriendCommand;
import com.kzzz3.argus.cortex.friend.application.FriendApplicationService;
import com.kzzz3.argus.cortex.shared.web.BearerTokenExtractor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

	private final FriendApplicationService friendApplicationService;

	public FriendController(FriendApplicationService friendApplicationService) {
		this.friendApplicationService = friendApplicationService;
	}

	@GetMapping
	public List<FriendResponse> listFriends(@RequestHeader("Authorization") String authorizationHeader) {
		return friendApplicationService.listFriends(BearerTokenExtractor.extract(authorizationHeader))
				.stream()
				.map(FriendResponse::from)
				.toList();
	}

	@PostMapping
	public FriendResponse addFriend(
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @RequestBody AddFriendRequest request
	) {
		return FriendResponse.from(friendApplicationService.addFriend(
				BearerTokenExtractor.extract(authorizationHeader),
				new AddFriendCommand(request.friendAccountId())
		));
	}
}
