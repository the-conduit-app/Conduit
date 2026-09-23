application = "../build/compose/binaries/main-release/app/Conduit.app"

files = [application]

symlinks = {
    "Applications": "/Applications",
}

icon_locations = {
    "Conduit.app": (150, 160),
    "Applications": (450, 160),
}

window_rect = ((100, 100), (600, 400))

icon_size = 96

background = "plasma_s64_arrow.png"
