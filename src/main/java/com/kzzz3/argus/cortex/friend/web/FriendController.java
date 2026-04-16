package com.kzzz3.argus.cortex.friend.web;

import com.kzzz3.argus.cortex.friend.application.AcceptFriendRequestCommand;
import com.kzzz3.argus.cortex.friend.application.AddFriendCommand;
import com.kzzz3.argus.cortex.friend.application.FriendApplicationService;
import com.kzzz3.argus.cortex.friend.application.IgnoreFriendRequestCommand;
import com.kzzz3.argus.cortex.friend.application.RejectFriendRequestCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	public List<FriendResponse> listFriends() {
		return friendApplicationService.listFriends()
				.stream()
				.map(FriendResponse::from)
				.toList();
	}

	@GetMapping("/requests")
	public PendingFriendRequestsResponse listPendingRequests() {
		return PendingFriendRequestsResponse.from(
				friendApplicationService.listPendingRequests()
		);
	}

	@PostMapping
	public FriendRequestResponse addFriend(
			@Valid @RequestBody AddFriendRequest request
	) {
		return FriendRequestResponse.from(friendApplicationService.sendFriendRequest(new AddFriendCommand(request.friendAccountId())));
	}

	@PostMapping("/requests/{requestId}/accept")
	public FriendResponse acceptFriendRequest(
			@PathVariable String requestId,
			@RequestBody(required = false) Object ignoredBody
	) {
		return FriendResponse.from(friendApplicationService.acceptFriendRequest(new AcceptFriendRequestCommand(requestId)));
	}

	@PostMapping("/requests/{requestId}/reject")
	public FriendRequestResponse rejectFriendRequest(
			@PathVariable String requestId,
			@RequestBody(required = false) Object ignoredBody
	) {
		return FriendRequestResponse.from(friendApplicationService.rejectFriendRequest(new RejectFriendRequestCommand(requestId)));
	}

	@PostMapping("/requests/{requestId}/ignore")
	public FriendRequestResponse ignoreFriendRequest(
			@PathVariable String requestId,
			@RequestBody(required = false) Object ignoredBody
	) {
		return FriendRequestResponse.from(friendApplicationService.ignoreFriendRequest(new IgnoreFriendRequestCommand(requestId)));
	}
}
