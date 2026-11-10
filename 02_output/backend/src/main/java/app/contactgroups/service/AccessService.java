package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import org.springframework.stereotype.Service;

/**
 * The access check of every group request (ADR-01). It holds the one place where the 404 is built
 * so that a non-member, a missing group, a malformed id and a group in Trash look the same (SR-03).
 * The membership and role checks come with the first story that has a group.
 */
@Service
public class AccessService {

  /** The one 404 of the API. */
  public static ApiException notFound() {
    return ApiException.notFound();
  }
}
