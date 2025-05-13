package io.genkt.mcp.common

@Suppress("ConstPropertyName")
public object McpConstants {
    public const val ProtocolVersion: String = "2025-03-26"
}

@Suppress("ConstPropertyName")
public object McpMethods {
    public const val Initialize: String = "initialize"
    public const val Ping: String = "ping"

    public object Resources {
        public const val List: String = "resources/list"
        public const val Read: String = "resources/read"
        public const val Subscribe: String = "resources/subscribe"
        public const val Unsubscribe: String = "resources/unsubscribe"

        public object Templates {
            public const val List: String = "resources/templates/list"
        }
    }

    public object Prompts {
        public const val List: String = "prompts/list"
        public const val Get: String = "prompts/get"
    }

    public object Tools {
        public const val List: String = "tools/list"
        public const val Call: String = "tools/call"
    }

    public object Sampling {
        public const val CreateMessage: String = "sampling/createMessage"
    }

    public object Completion {
        public const val Complete: String = "completion/complete"
    }

    public object Roots {
        public const val List: String = "roots/list"
    }

    public object Logging {
        public const val SetLevel: String = "logging/setLevel"
    }

    public object Notifications {
        public const val Initialized: String = "notifications/initialized"
        public const val Cancelled: String = "notifications/cancelled"
        public const val Progress: String = "notifications/progress"
        public const val Message: String = "notifications/message"

        public object Resources {
            public const val Updated: String = "notifications/resources/updated"
            public const val ListChanged: String = "notifications/resources/list_changed"
        }

        public object Tools {
            public const val ListChanged: String = "notifications/tools/list_changed"
        }

        public object Roots {
            public const val ListChanged: String = "notifications/roots/list_changed"
        }

        public object Prompts {
            public const val ListChanged: String = "notifications/prompts/list_changed"
        }
    }
}