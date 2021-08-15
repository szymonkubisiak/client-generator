package models

/**
 * Representation of something that can end up as a group of API calls
 * So far: tag (as group of endpoints) or endpoint (flying solo)
 */
interface EndpointGroup {
	val key: String
}