package com.dualprofile.app
import com.dualprofile.app.profile.canChangeOwnWorkRestriction
import org.junit.Assert.*
import org.junit.Test
class PolicyConsentTest {
 @Test fun noConsentLeavesPolicyUnchanged() { assertFalse(canChangeOwnWorkRestriction(true,true,true,false)) }
 @Test fun personalAndNonOwnerCannotChangePolicy() { assertFalse(canChangeOwnWorkRestriction(true,false,true,true)); assertFalse(canChangeOwnWorkRestriction(false,true,true,true)) }
 @Test fun otherAdministratorsRestrictionCannotBeCleared() { assertFalse(canChangeOwnWorkRestriction(true,true,false,true)) }
 @Test fun ownerMayChangeOnlyOwnWorkPolicyAfterConsent() { assertTrue(canChangeOwnWorkRestriction(true,true,true,true)) }
}
