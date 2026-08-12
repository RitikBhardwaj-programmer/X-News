package com.cfs.xnews.user;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public Map<String, String> getCurrentUser(
            Authentication authentication
    ) {

        return Map.of(
                "email",
                authentication.getName()
        );
    }
}
