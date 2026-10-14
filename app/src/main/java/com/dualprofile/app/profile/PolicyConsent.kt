package com.dualprofile.app.profile

fun canChangeOwnWorkRestriction(profileOwner: Boolean, managedProfile: Boolean, ownRestriction: Boolean, explicitConsent: Boolean) = profileOwner && managedProfile && ownRestriction && explicitConsent
